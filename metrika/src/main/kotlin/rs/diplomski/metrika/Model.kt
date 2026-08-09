package rs.diplomski.metrika

/**
 * Domenski model metričkog sloja (faza 3).
 *
 * Sve je namerno jednostavno i BEZ I/O — čist podatak, da bi parsiranje i
 * računanje bili testabilni odvojeno od čitanja fajlova.
 */

/** Jedan lint nalaz koji NAS zanima (posle filtriranja na naša 4 ID-a). */
data class Nalaz(
    val idPravila: String,
    val ozbiljnost: String,
    val fajl: String,
    val linija: Int,
    val poruka: String,
)

/**
 * Rezultat parsiranja lint izveštaja: naši nalazi + kontekst koliko je
 * tuđih (ne-naših) upozorenja bilo u istom izveštaju.
 */
data class ParsiranIzvestaj(
    val nasiNalazi: List<Nalaz>,
    val ukupnoTudjih: Int,
) {
    /** Broj nalaza po SVAKOM našem pravilu (uvek sva 4, i kad je 0). */
    fun poPravilu(): Map<String, Int> =
        Pravila.REDOSLED.associateWith { id -> nasiNalazi.count { it.idPravila == id } }
}

/** Kanonski identifikatori naša 4 isporučena pravila i njihov redosled. */
object Pravila {
    const val STANJE = "StanjeBezRemember"
    const val ALOKACIJA = "SkupaAlokacijaBezRemember"
    const val NESTABILAN = "NestabilanTipParametra"
    const val HARDKOD = "HardkodovaniString"

    /** Fiksan redosled za sve izlaze — da kolone CSV-a nikad ne „plešu". */
    val REDOSLED: List<String> = listOf(STANJE, ALOKACIJA, NESTABILAN, HARDKOD)
    val SKUP: Set<String> = REDOSLED.toSet()
}

/** Jasna, namenska greška — da malformiran ulaz padne glasno, ne tiho. */
class MetrikaGreska(poruka: String, uzrok: Throwable? = null) :
    RuntimeException(poruka, uzrok)
