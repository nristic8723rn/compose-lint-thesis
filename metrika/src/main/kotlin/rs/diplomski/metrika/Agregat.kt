package rs.diplomski.metrika

/**
 * Računanje agregata (faza 3c — trojna metrika). Bez I/O.
 *
 * Za svako pravilo vodimo TRI broja (ukupno/zatečeno/novo) i za svaki od njih
 * računamo ponderisani zbir i normalizovane vrednosti. Time metrika može da
 * pokaže SMANJENJE duga (ukupan pada) i efekat kapije (novi stoji na nuli).
 */
object Metrika {

    /** Ponderisani zbir za dati skup brojeva po pravilu. */
    fun ponderisaniZbir(poPravilu: Map<String, Int>, ponderi: Map<String, Ponder>): Double =
        poPravilu.entries.sumOf { (id, broj) -> broj * (ponderi[id]?.vrednost ?: 0.0) }

    /**
     * Sastavi mernu tačku iz UKUPNOG izveštaja (lint bez baseline-a) i,
     * opciono, baseline izveštaja (zatečeno). Ako baseline nije prosleđen:
     * zatečeno = 0, novo = ukupno, [MernaTacka.baselinePrisutan] = false.
     *
     * Deljenje nulom (kloc=0 ili @Composable=0) → normalizovana vrednost je
     * null (prazno), ne 0 — odsustvo podatka nije „nula duga" (odluka 10).
     */
    fun mernaTacka(
        projekat: String,
        commitHash: String,
        datum: String,
        ukupanIzvestaj: ParsiranIzvestaj,
        baseline: ParsiranIzvestaj?,
        norm: Normalizacija,
        ponderi: Map<String, Ponder>,
    ): MernaTacka {
        val ukupnoMap = ukupanIzvestaj.poPravilu()
        val zatecenoMap = baseline?.poPravilu() ?: Pravila.REDOSLED.associateWith { 0 }
        val poPravilu = Pravila.REDOSLED.associateWith { id ->
            TriBroja.izracunaj(ukupnoMap.getValue(id), zatecenoMap.getValue(id))
        }

        val zbirU = ponderisaniZbir(poPravilu.mapValues { it.value.ukupno }, ponderi)
        val zbirZ = ponderisaniZbir(poPravilu.mapValues { it.value.zateceno }, ponderi)
        val zbirN = ponderisaniZbir(poPravilu.mapValues { it.value.novo }, ponderi)

        val poKloc = if (norm.kloc > 0.0) {
            TriDouble(zbirU / norm.kloc, zbirZ / norm.kloc, zbirN / norm.kloc)
        } else null
        val poComposable = if (norm.brojComposable > 0) {
            val n = norm.brojComposable.toDouble()
            TriDouble(zbirU / n, zbirZ / n, zbirN / n)
        } else null

        return MernaTacka(
            projekat = projekat,
            commitHash = commitHash,
            datum = datum,
            kloc = norm.kloc,
            brojComposable = norm.brojComposable,
            poPravilu = poPravilu,
            baselinePrisutan = baseline != null,
            ponderisaniZbir = TriDouble(zbirU, zbirZ, zbirN),
            dugPoKloc = poKloc,
            dugPoComposable = poComposable,
            ukupnoTudjih = ukupanIzvestaj.ukupnoTudjih,
        )
    }
}
