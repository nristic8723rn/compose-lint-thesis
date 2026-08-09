package rs.diplomski.metrika

/**
 * Agregatna (izračunata) metrika tehničkog duga za jednu mernu tačku.
 *  - ponderisaniZbir = Σ (broj_nalaza[pravilo] × ponder[pravilo])
 *  - dugPoKloc        = ponderisaniZbir / KLOC
 *  - dugPoComposable  = ponderisaniZbir / broj @Composable
 */
data class Agregat(
    val ponderisaniZbir: Double,
    val dugPoKloc: Double,
    val dugPoComposable: Double,
)

object Metrika {

    /**
     * Čist izračun agregata (bez I/O).
     *
     * OTVORENO PITANJE (ostavljeno za chat, ne presecam): ako merna tačka nema
     * .kt fajlova (kloc = 0) ili nema @Composable (0), delilac je 0. Privremeno
     * vraćamo 0.0 za taj odnos; da li je ispravnije 0, null ili izostaviti red
     * — odluka za chat. Videti docs/faza3-metrika.md.
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
            dugPoKloc = if (kloc > 0.0) zbir / kloc else 0.0,
            dugPoComposable = if (brojComposable > 0) zbir / brojComposable else 0.0,
        )
    }
}
