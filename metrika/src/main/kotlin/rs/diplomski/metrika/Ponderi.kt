package rs.diplomski.metrika

/**
 * ZADATAK 3: ponderi po pravilu, izvedeni iz MERILA (ne izmišljeni).
 *
 *   ponder = UTICAJ × POUZDANOST
 *   UTICAJ:     korektnost = 3 | performanse = 2 | održivost/proces = 1
 *   POUZDANOST: visoka = 1.0   | srednja = 0.7    | niska = 0.4
 *
 * Vrednosti su KONFIGURABILNE (config fajl / CLI), da bi se u radu mogla
 * pokazati osetljivost metrike na izbor pondera — nisu zakucane u agregat.
 */

enum class Uticaj(val vrednost: Int) {
    KOREKTNOST(3),
    PERFORMANSE(2),
    ODRZIVOST(1),
}

enum class Pouzdanost(val vrednost: Double) {
    VISOKA(1.0),
    SREDNJA(0.7),
    NISKA(0.4),
}

data class Ponder(val uticaj: Int, val pouzdanost: Double) {
    val vrednost: Double get() = uticaj * pouzdanost
}

object Ponderi {

    /**
     * Podrazumevana dodela, sa obrazloženjem po pravilu (isto i u docs):
     *  - StanjeBezRemember: korektnost (3) × pouzdana detekcija pozitivnim
     *    dokazom (1.0) = 3.0 — najozbiljnije, blokira merge.
     *  - SkupaAlokacijaBezRemember: performanse (2) × srednja pouzdanost jer
     *    radi sa listom tipova, ne semantikom (0.7) = 1.4.
     *  - NestabilanTipParametra: performanse (2) × srednja pouzdanost jer je
     *    tačno ali bučno na realnom kodu (0.7) = 1.4.
     *  - HardkodovaniString: održivost/proces (1) × pouzdana detekcija inline
     *    literala (1.0) = 1.0.
     */
    val PODRAZUMEVANI: Map<String, Ponder> = mapOf(
        Pravila.STANJE to Ponder(Uticaj.KOREKTNOST.vrednost, Pouzdanost.VISOKA.vrednost),
        Pravila.ALOKACIJA to Ponder(Uticaj.PERFORMANSE.vrednost, Pouzdanost.SREDNJA.vrednost),
        Pravila.NESTABILAN to Ponder(Uticaj.PERFORMANSE.vrednost, Pouzdanost.SREDNJA.vrednost),
        Pravila.HARDKOD to Ponder(Uticaj.ODRZIVOST.vrednost, Pouzdanost.VISOKA.vrednost),
    )

    /**
     * Učitaj pondere iz prostog teksta (config fajl ili CLI), red po red:
     *   `IdPravila = UTICAJ x POUZDANOST`   (npr. `HardkodovaniString = 2 x 1.0`)
     * Prazni redovi i `#` komentari se preskaču. Nepoznat ID je dozvoljen
     * (dodaje se). Neispravan red pada [MetrikaGreska]-om (ne tiho).
     * Redovi koji nisu navedeni zadržavaju vrednost iz [osnova].
     */
    fun izTeksta(tekst: String, osnova: Map<String, Ponder> = PODRAZUMEVANI): Map<String, Ponder> {
        val rezultat = osnova.toMutableMap()
        for (linija in tekst.lineSequence()) {
            val t = linija.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val delovi = t.split("=", limit = 2)
            if (delovi.size != 2) throw MetrikaGreska("Neispravan red pondera: '$linija'")
            val id = delovi[0].trim()
            val izraz = delovi[1].split("x", "X", "*").map { it.trim() }
            if (izraz.size != 2) {
                throw MetrikaGreska("Ponder mora biti 'UTICAJ x POUZDANOST': '$linija'")
            }
            val uticaj = izraz[0].toIntOrNull()
                ?: throw MetrikaGreska("Uticaj mora biti ceo broj: '$linija'")
            val pouzdanost = izraz[1].toDoubleOrNull()
                ?: throw MetrikaGreska("Pouzdanost mora biti broj: '$linija'")
            rezultat[id] = Ponder(uticaj, pouzdanost)
        }
        return rezultat
    }
}
