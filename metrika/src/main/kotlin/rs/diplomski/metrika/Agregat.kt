package rs.diplomski.metrika

/**
 * Agregatna (izračunata) metrika tehničkog duga za jednu mernu tačku.
 *  - ponderisaniZbir = Σ (broj_nalaza[pravilo] × ponder[pravilo])
 *  - dugPoKloc        = ponderisaniZbir / KLOC
 *  - dugPoComposable  = ponderisaniZbir / broj @Composable
 */
data class Agregat(
    val ponderisaniZbir: Double,
    // null (ne 0.0) kad je delilac 0 — odsustvo podatka, ne „nula duga".
    // Tačka se u grafikonu preskače (prekid linije), red ostaje.
    val dugPoKloc: Double?,
    val dugPoComposable: Double?,
)

object Metrika {

    /**
     * Čist izračun agregata (bez I/O).
     *
     * ODLUKA (faza 3b): ako merna tačka nema .kt fajlova (kloc = 0) ili nema
     * @Composable (0), delilac je 0 pa je odnos NULL (prazno), ne 0.0 — odsustvo
     * podatka nije „nula duga". U CSV-u prazno polje, u JSON-u null, u grafikonu
     * prekid linije.
     */
    fun agregiraj(
        poPravilu: Map<String, Int>,
        ponderi: Map<String, Ponder>,
        kloc: Double,
        brojComposable: Int,
    ): Agregat {
        val zbir = poPravilu.entries.sumOf { (id, broj) ->
            broj * (ponderi[id]?.vrednost ?: 0.0)
        }
        return Agregat(
            ponderisaniZbir = zbir,
            dugPoKloc = if (kloc > 0.0) zbir / kloc else null,
            dugPoComposable = if (brojComposable > 0) zbir / brojComposable else null,
        )
    }

    /** Sastavi jednu mernu tačku iz parsiranog izveštaja + normalizacije. */
    fun mernaTacka(
        projekat: String,
        commitHash: String,
        datum: String,
        izvestaj: ParsiranIzvestaj,
        norm: Normalizacija,
        ponderi: Map<String, Ponder>,
    ): MernaTacka {
        val poPravilu = izvestaj.poPravilu()
        val a = agregiraj(poPravilu, ponderi, norm.kloc, norm.brojComposable)
        return MernaTacka(
            projekat = projekat,
            commitHash = commitHash,
            datum = datum,
            kloc = norm.kloc,
            brojComposable = norm.brojComposable,
            poPravilu = poPravilu,
            ponderisaniZbir = a.ponderisaniZbir,
            dugPoKloc = a.dugPoKloc,
            dugPoComposable = a.dugPoComposable,
            ukupnoTudjih = izvestaj.ukupnoTudjih,
        )
    }
}
