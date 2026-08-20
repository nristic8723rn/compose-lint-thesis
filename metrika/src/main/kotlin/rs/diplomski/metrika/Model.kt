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

/**
 * Tri broja po pravilu (faza 3c — trojna metrika):
 *  - [ukupno]:   svi nalazi (lint bez baseline-a)
 *  - [zateceno]: broj stavki u lint-baseline.xml za to pravilo
 *  - [novo]:     ukupno - zatečeno (clampovano na 0)
 *
 * [baselineZastario] je true kad je zatečeno > ukupno — baseline sadrži stavke
 * kojih više nema, tj. dug je otplaćen a baseline nije osvežen. To je KORISNA
 * informacija (ne greška): signal da baseline treba osvežiti.
 */
data class TriBroja(
    val ukupno: Int,
    val zateceno: Int,
    val novo: Int,
    val baselineZastario: Boolean,
) {
    companion object {
        fun izracunaj(ukupno: Int, zateceno: Int): TriBroja {
            val razlika = ukupno - zateceno
            return TriBroja(
                ukupno = ukupno,
                zateceno = zateceno,
                novo = maxOf(0, razlika),
                baselineZastario = razlika < 0,
            )
        }
    }
}

/** Tri agregatne/normalizovane vrednosti (ukupno/zatečeno/novo). */
data class TriDouble(val ukupno: Double, val zateceno: Double, val novo: Double)
