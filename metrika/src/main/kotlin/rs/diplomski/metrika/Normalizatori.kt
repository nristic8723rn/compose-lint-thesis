package rs.diplomski.metrika

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/**
 * ZADATAK 2: dva normalizatora (oba se uvek računaju).
 *  (a) KLOC — broj nepraznih linija u .kt fajlovima / 1000.
 *  (b) BROJ_COMPOSABLE — broj @Composable anotacija u istim fajlovima.
 *
 * Isključuju se: build/, test izvori (src/test, src/androidTest) i generisani
 * kod (segment „generated"). Brojači su ČISTI (rade nad tekstom), a I/O sloj
 * (obilazak stabla) je odvojen — zato su brojači testabilni bez fajl sistema.
 */
data class Normalizacija(
    val brojLinija: Int,
    val kloc: Double,
    val brojComposable: Int,
)

object Normalizatori {

    private val POJAVA_COMPOSABLE = Regex("""@Composable\b""")

    // /* ... */ preko više linija — DOT_MATCHES_ALL da uhvati i višelinijske.
    private val BLOK_KOMENTAR = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)

    /** Broj nepraznih linija u datom tekstu. */
    fun prebrojNepneprazneLinije(tekst: String): Int =
        tekst.lineSequence().count { it.isNotBlank() }

    /**
     * Broj @Composable anotacija u tekstu.
     *
     * NAMERNO JEFTINA APROKSIMACIJA (dokumentovano ograničenje): ne parsiramo
     * AST, nego brojimo po tekstu. Preskačemo importe (linija počinje sa
     * `import `), linijske komentare (deo posle `//`) i blok komentare
     * (`/* ... */`). Ivične slučajeve (npr. `@Composable` u string literalu)
     * svesno ne pokrivamo — cena bi bila pun parser.
     */
    fun prebrojComposable(tekst: String): Int {
        val bezBlokKomentara = tekst.replace(BLOK_KOMENTAR, "")
        var broj = 0
        for (linija in bezBlokKomentara.lineSequence()) {
            val t = linija.trim()
            if (t.startsWith("import ")) continue
            val bezLinijskog = t.substringBefore("//")
            broj += POJAVA_COMPOSABLE.findAll(bezLinijskog).count()
        }
        return broj
    }

    /** Obilazak izvornog stabla i zbir oba normalizatora nad .kt fajlovima. */
    fun izbrojNad(koren: Path): Normalizacija {
        var linije = 0
        var composable = 0
        for (fajl in ktFajlovi(koren)) {
            val tekst = fajl.readText()
            linije += prebrojNepneprazneLinije(tekst)
            composable += prebrojComposable(tekst)
        }
        return Normalizacija(brojLinija = linije, kloc = linije / 1000.0, brojComposable = composable)
    }

    /** Svi .kt fajlovi pod korenom, bez build/, test izvora i generisanog koda. */
    fun ktFajlovi(koren: Path): List<Path> =
        Files.walk(koren).use { tok ->
            tok.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .filter { !jePutanjaIskljucena(koren.relativize(it).toString()) }
                .sorted()
                .toList()
        }

    /**
     * true ako putanju (relativnu, u odnosu na koren) treba isključiti:
     * segment `build` ili `generated`, ili test izvori (src/test, src/androidTest).
     * Javna je da bi se isključivanje testiralo direktno, bez fajlova na disku
     * (fixture `build/` bi bio gitignore-ovan).
     */
    fun jePutanjaIskljucena(relativnaPutanja: String): Boolean {
        val segmenti = relativnaPutanja.replace('\\', '/').split('/')
        if ("build" in segmenti) return true
        if ("generated" in segmenti) return true
        val idxSrc = segmenti.indexOf("src")
        if (idxSrc >= 0 && idxSrc + 1 < segmenti.size &&
            segmenti[idxSrc + 1] in setOf("test", "androidTest")
        ) {
            return true
        }
        return false
    }
}
