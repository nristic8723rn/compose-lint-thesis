package rs.diplomski.metrika

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.system.exitProcess

/**
 * CLI ulazna tačka metričkog sloja. Dve komande:
 *
 *   izvestaj  --xml <lint.xml> --izvor <koren> --commit <hash>
 *             [--datum yyyy-MM-dd] [--ponderi <config>] --csv <out.csv> [--json <out.jsonl>]
 *
 *   trend     --csv <in.csv> --html <out.html>
 *
 * Sva „pamet" je u čistim funkcijama (parser/normalizatori/agregat/zapis);
 * ovde je samo I/O i sklapanje.
 */
fun main(args: Array<String>) {
    try {
        when (args.firstOrNull()) {
            "izvestaj" -> komandaIzvestaj(zastavice(args.drop(1)))
            "trend" -> komandaTrend(zastavice(args.drop(1)))
            else -> {
                pomoc()
                exitProcess(if (args.isEmpty()) 0 else 1)
            }
        }
    } catch (e: MetrikaGreska) {
        System.err.println("GREŠKA: ${e.message}")
        exitProcess(2)
    }
}

private fun komandaIzvestaj(z: Map<String, String>) {
    val xml = Files.readString(putanja(z, "--xml"))
    val koren = putanja(z, "--izvor")
    val commit = obavezno(z, "--commit")
    val datum = z["--datum"] ?: LocalDate.now().toString()
    val ponderi = z["--ponderi"]?.let { Ponderi.izTeksta(Files.readString(Path.of(it))) }
        ?: Ponderi.PODRAZUMEVANI

    val izvestaj = LintParser.parsiraj(xml)
    val norm = Normalizatori.izbrojNad(koren)
    val tacka = Metrika.mernaTacka(commit, datum, izvestaj, norm, ponderi)

    Zapis.dopisiCsv(putanja(z, "--csv"), tacka)
    z["--json"]?.let { staza ->
        val p = Path.of(staza)
        p.parent?.let { Files.createDirectories(it) }
        Files.write(
            p,
            (Zapis.jsonRed(tacka) + System.lineSeparator()).toByteArray(),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND,
        )
    }

    println("Merna tačka upisana (commit=$commit):")
    println("  KLOC=${"%.3f".format(java.util.Locale.ROOT, norm.kloc)} @Composable=${norm.brojComposable}")
    println("  po pravilu: " + Pravila.REDOSLED.joinToString(", ") { "$it=${tacka.poPravilu[it]}" })
    println("  ponderisani_zbir=${"%.2f".format(java.util.Locale.ROOT, tacka.ponderisaniZbir)}" +
        " dug/KLOC=${"%.2f".format(java.util.Locale.ROOT, tacka.dugPoKloc)}" +
        " dug/@Composable=${"%.2f".format(java.util.Locale.ROOT, tacka.dugPoComposable)}")
    println("  tuđih upozorenja (kontekst): ${tacka.ukupnoTudjih}")
}

private fun komandaTrend(z: Map<String, String>) {
    val tacke = Zapis.ucitajCsv(Files.readString(putanja(z, "--csv")))
    val html = TrendIzvestaj.generisi(tacke, LocalDate.now().toString())
    val izlaz = putanja(z, "--html")
    izlaz.parent?.let { Files.createDirectories(it) }
    Files.writeString(izlaz, html)
    println("Trend izveštaj generisan: $izlaz (${tacke.size} mernih tačaka)")
}

/** Prosti parser `--kljuc vrednost` parova. */
private fun zastavice(delovi: List<String>): Map<String, String> {
    val mapa = mutableMapOf<String, String>()
    var i = 0
    while (i < delovi.size) {
        val kljuc = delovi[i]
        if (!kljuc.startsWith("--")) throw MetrikaGreska("Očekivan --parametar, dobijeno: '$kljuc'")
        val vrednost = delovi.getOrNull(i + 1)
            ?: throw MetrikaGreska("Parametar '$kljuc' nema vrednost")
        mapa[kljuc] = vrednost
        i += 2
    }
    return mapa
}

private fun obavezno(z: Map<String, String>, kljuc: String): String =
    z[kljuc] ?: throw MetrikaGreska("Nedostaje obavezan parametar $kljuc")

private fun putanja(z: Map<String, String>, kljuc: String): Path = Path.of(obavezno(z, kljuc))

private fun pomoc() {
    println(
        """
        metrika — metrički sloj (faza 3)

        Komande:
          izvestaj --xml <lint.xml> --izvor <koren> --commit <hash>
                   [--datum yyyy-MM-dd] [--ponderi <config>] --csv <out.csv> [--json <out.jsonl>]
          trend    --csv <in.csv> --html <out.html>
        """.trimIndent(),
    )
}
