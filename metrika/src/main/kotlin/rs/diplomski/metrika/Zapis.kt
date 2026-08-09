package rs.diplomski.metrika

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale

/**
 * ZADATAK 4: izlazni zapis — jedan red po mernoj tački, u CSV (dopisivanje)
 * i isti sadržaj kao JSON red. Bez baze.
 *
 * Decimale se formatiraju sa Locale.ROOT (tačka), da srpski locale ne ubaci
 * zarez i ne pokvari CSV (koji je zarezom razdvojen).
 */
data class MernaTacka(
    val projekat: String,
    val commitHash: String,
    val datum: String,
    val kloc: Double,
    val brojComposable: Int,
    val poPravilu: Map<String, Int>,
    val ponderisaniZbir: Double,
    // null (prazno) kad je delilac 0 — vidi Metrika.agregiraj.
    val dugPoKloc: Double?,
    val dugPoComposable: Double?,
    val ukupnoTudjih: Int,
)

object Zapis {

    /** Fiksne kolone CSV-a — pravila u kanonskom redosledu, uvek sva 4. */
    val CSV_KOLONE: List<String> =
        listOf("projekat", "commit_hash", "datum", "kloc", "broj_composable") +
            Pravila.REDOSLED +
            listOf("ponderisani_zbir", "dug_po_kloc", "dug_po_composable", "ukupno_tudjih_upozorenja")

    fun csvZaglavlje(): String = CSV_KOLONE.joinToString(",")

    fun csvRed(t: MernaTacka): String {
        val polja = buildList {
            add(t.projekat)
            add(t.commitHash)
            add(t.datum)
            add(fmt(t.kloc))
            add(t.brojComposable.toString())
            Pravila.REDOSLED.forEach { add((t.poPravilu[it] ?: 0).toString()) }
            add(fmt(t.ponderisaniZbir))
            add(fmtOpt(t.dugPoKloc))       // prazno polje kad je null
            add(fmtOpt(t.dugPoComposable)) // prazno polje kad je null
            add(t.ukupnoTudjih.toString())
        }
        return polja.joinToString(",")
    }

    fun jsonRed(t: MernaTacka): String {
        val poPravilu = Pravila.REDOSLED.joinToString(",") { "\"$it\":${t.poPravilu[it] ?: 0}" }
        return "{" +
            "\"projekat\":\"${t.projekat}\"," +
            "\"commit_hash\":\"${t.commitHash}\"," +
            "\"datum\":\"${t.datum}\"," +
            "\"kloc\":${fmt(t.kloc)}," +
            "\"broj_composable\":${t.brojComposable}," +
            "\"po_pravilu\":{$poPravilu}," +
            "\"ponderisani_zbir\":${fmt(t.ponderisaniZbir)}," +
            "\"dug_po_kloc\":${jsonOpt(t.dugPoKloc)}," +      // null kad je prazno
            "\"dug_po_composable\":${jsonOpt(t.dugPoComposable)}," +
            "\"ukupno_tudjih_upozorenja\":${t.ukupnoTudjih}" +
            "}"
    }

    /** I/O: dopiši red u CSV; zaglavlje se piše samo kad je fajl nov/prazan. */
    fun dopisiCsv(putanja: Path, t: MernaTacka) {
        val trebaZaglavlje = !Files.exists(putanja) || Files.size(putanja) == 0L
        putanja.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(
            putanja,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        ).use { w ->
            if (trebaZaglavlje) {
                w.write(csvZaglavlje())
                w.newLine()
            }
            w.write(csvRed(t))
            w.newLine()
        }
    }

    /** Učitaj CSV (sa zaglavljem) natrag u merne tačke — ulaz za trend. */
    fun ucitajCsv(tekst: String): List<MernaTacka> {
        val linije = tekst.lineSequence().filter { it.isNotBlank() }.toList()
        if (linije.isEmpty()) return emptyList()
        val zaglavlje = linije.first().split(",")
        val idx = zaglavlje.withIndex().associate { (i, ime) -> ime to i }
        fun kol(polja: List<String>, ime: String): String {
            val i = idx[ime] ?: throw MetrikaGreska("CSV nema kolonu '$ime'")
            return polja[i]
        }
        return linije.drop(1).map { red ->
            val p = red.split(",")
            MernaTacka(
                projekat = kol(p, "projekat"),
                commitHash = kol(p, "commit_hash"),
                datum = kol(p, "datum"),
                kloc = kol(p, "kloc").toDouble(),
                brojComposable = kol(p, "broj_composable").toInt(),
                poPravilu = Pravila.REDOSLED.associateWith { kol(p, it).toInt() },
                ponderisaniZbir = kol(p, "ponderisani_zbir").toDouble(),
                dugPoKloc = kol(p, "dug_po_kloc").ifBlank { null }?.toDouble(),
                dugPoComposable = kol(p, "dug_po_composable").ifBlank { null }?.toDouble(),
                ukupnoTudjih = kol(p, "ukupno_tudjih_upozorenja").toInt(),
            )
        }
    }

    private fun fmt(x: Double): String = String.format(Locale.ROOT, "%.4f", x)

    /** Prazno polje (CSV) kad je vrednost null. */
    private fun fmtOpt(x: Double?): String = if (x == null) "" else fmt(x)

    /** JSON null kad je vrednost odsutna. */
    private fun jsonOpt(x: Double?): String = if (x == null) "null" else fmt(x)
}
