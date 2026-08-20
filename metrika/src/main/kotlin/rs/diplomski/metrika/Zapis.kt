package rs.diplomski.metrika

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale

/**
 * Izlazni zapis (faza 3c — trojna metrika). Jedan red po mernoj tački, u CSV
 * (dopisivanje) i isti sadržaj kao JSON red.
 *
 * Za svako pravilo TRI kolone (ukupno/zatečeno/novo), plus tri agregata i tri
 * normalizovane vrednosti po svakom normalizatoru. Šema je dokumentovana u
 * docs/faza3-metrika.md. Decimale sa Locale.ROOT (tačka), prazno polje kad je
 * normalizovana vrednost null (deljenje nulom).
 */
data class MernaTacka(
    val projekat: String,
    val commitHash: String,
    val datum: String,
    val kloc: Double,
    val brojComposable: Int,
    val poPravilu: Map<String, TriBroja>,
    val baselinePrisutan: Boolean,
    val ponderisaniZbir: TriDouble,
    val dugPoKloc: TriDouble?,        // null kad je kloc = 0
    val dugPoComposable: TriDouble?,  // null kad je broj @Composable = 0
    val ukupnoTudjih: Int,
) {
    /** true ako bar jedno pravilo ima zastareo baseline (zatečeno > ukupno). */
    fun baselineZastario(): Boolean = poPravilu.values.any { it.baselineZastario }
}

object Zapis {

    val CSV_KOLONE: List<String> = buildList {
        addAll(listOf("projekat", "commit_hash", "datum", "kloc", "broj_composable", "baseline_prisutan"))
        Pravila.REDOSLED.forEach { addAll(listOf("${it}_ukupno", "${it}_zateceno", "${it}_novo")) }
        addAll(listOf("ponderisani_zbir_ukupno", "ponderisani_zbir_zateceno", "ponderisani_zbir_novo"))
        addAll(listOf("dug_po_kloc_ukupno", "dug_po_kloc_zateceno", "dug_po_kloc_novo"))
        addAll(listOf("dug_po_composable_ukupno", "dug_po_composable_zateceno", "dug_po_composable_novo"))
        add("ukupno_tudjih_upozorenja")
    }

    fun csvZaglavlje(): String = CSV_KOLONE.joinToString(",")

    fun csvRed(t: MernaTacka): String {
        val polja = buildList {
            add(t.projekat)
            add(t.commitHash)
            add(t.datum)
            add(fmt(t.kloc))
            add(t.brojComposable.toString())
            add(t.baselinePrisutan.toString())
            Pravila.REDOSLED.forEach {
                val tb = t.poPravilu.getValue(it)
                add(tb.ukupno.toString()); add(tb.zateceno.toString()); add(tb.novo.toString())
            }
            addAll(triCsv(t.ponderisaniZbir))
            addAll(triCsvOpt(t.dugPoKloc))
            addAll(triCsvOpt(t.dugPoComposable))
            add(t.ukupnoTudjih.toString())
        }
        return polja.joinToString(",")
    }

    fun jsonRed(t: MernaTacka): String {
        val poPravilu = Pravila.REDOSLED.joinToString(",") { id ->
            val tb = t.poPravilu.getValue(id)
            "\"$id\":{\"ukupno\":${tb.ukupno},\"zateceno\":${tb.zateceno},\"novo\":${tb.novo}}"
        }
        return "{" +
            "\"projekat\":\"${t.projekat}\"," +
            "\"commit_hash\":\"${t.commitHash}\"," +
            "\"datum\":\"${t.datum}\"," +
            "\"kloc\":${fmt(t.kloc)}," +
            "\"broj_composable\":${t.brojComposable}," +
            "\"baseline_prisutan\":${t.baselinePrisutan}," +
            "\"po_pravilu\":{$poPravilu}," +
            "\"ponderisani_zbir\":${triJson(t.ponderisaniZbir)}," +
            "\"dug_po_kloc\":${triJsonOpt(t.dugPoKloc)}," +
            "\"dug_po_composable\":${triJsonOpt(t.dugPoComposable)}," +
            "\"ukupno_tudjih_upozorenja\":${t.ukupnoTudjih}" +
            "}"
    }

    fun dopisiCsv(putanja: Path, t: MernaTacka) {
        val trebaZaglavlje = !Files.exists(putanja) || Files.size(putanja) == 0L
        putanja.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(putanja, StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { w ->
            if (trebaZaglavlje) { w.write(csvZaglavlje()); w.newLine() }
            w.write(csvRed(t)); w.newLine()
        }
    }

    fun ucitajCsv(tekst: String): List<MernaTacka> {
        val linije = tekst.lineSequence().filter { it.isNotBlank() }.toList()
        if (linije.isEmpty()) return emptyList()
        val idx = linije.first().split(",").withIndex().associate { (i, ime) -> ime to i }
        fun kol(p: List<String>, ime: String): String {
            val i = idx[ime] ?: throw MetrikaGreska("CSV nema kolonu '$ime'")
            return p[i]
        }
        return linije.drop(1).map { red ->
            val p = red.split(",")
            MernaTacka(
                projekat = kol(p, "projekat"),
                commitHash = kol(p, "commit_hash"),
                datum = kol(p, "datum"),
                kloc = kol(p, "kloc").toDouble(),
                brojComposable = kol(p, "broj_composable").toInt(),
                poPravilu = Pravila.REDOSLED.associateWith { id ->
                    TriBroja.izracunaj(kol(p, "${id}_ukupno").toInt(), kol(p, "${id}_zateceno").toInt())
                },
                baselinePrisutan = kol(p, "baseline_prisutan").toBoolean(),
                ponderisaniZbir = triCitaj(p, ::kol, "ponderisani_zbir")!!,
                dugPoKloc = triCitaj(p, ::kol, "dug_po_kloc"),
                dugPoComposable = triCitaj(p, ::kol, "dug_po_composable"),
                ukupnoTudjih = kol(p, "ukupno_tudjih_upozorenja").toInt(),
            )
        }
    }

    private fun triCitaj(p: List<String>, kol: (List<String>, String) -> String, prefiks: String): TriDouble? {
        val u = kol(p, "${prefiks}_ukupno")
        if (u.isBlank()) return null
        return TriDouble(u.toDouble(), kol(p, "${prefiks}_zateceno").toDouble(), kol(p, "${prefiks}_novo").toDouble())
    }

    private fun fmt(x: Double): String = String.format(Locale.ROOT, "%.4f", x)
    private fun triCsv(t: TriDouble): List<String> = listOf(fmt(t.ukupno), fmt(t.zateceno), fmt(t.novo))
    private fun triCsvOpt(t: TriDouble?): List<String> = if (t == null) listOf("", "", "") else triCsv(t)
    private fun triJson(t: TriDouble): String =
        "{\"ukupno\":${fmt(t.ukupno)},\"zateceno\":${fmt(t.zateceno)},\"novo\":${fmt(t.novo)}}"
    private fun triJsonOpt(t: TriDouble?): String = if (t == null) "null" else triJson(t)
}
