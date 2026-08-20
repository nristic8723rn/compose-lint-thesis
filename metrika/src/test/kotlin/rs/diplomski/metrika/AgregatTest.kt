package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEST-KORPUS za agregat i trojnu metriku (test-first). Naglasak: granični
 * slučaj negativnog „novo" (baseline zastareo) i računanje tri agregata.
 */
class AgregatTest {

    private fun izvestaj(vararg parovi: Pair<String, Int>, tudjih: Int = 0): ParsiranIzvestaj {
        val nalazi = parovi.flatMap { (id, n) -> List(n) { Nalaz(id, "Warning", "F.kt", 1, "m") } }
        return ParsiranIzvestaj(nalazi, tudjih)
    }

    // -------- granični slučaj: TriBroja --------

    @Test
    fun `TriBroja - normalno racunanje novo`() {
        val tb = TriBroja.izracunaj(ukupno = 5, zateceno = 2)
        assertEquals(3, tb.novo)
        assertFalse(tb.baselineZastario)
    }

    @Test
    fun `TriBroja - zateceno vece od ukupno daje novo 0 + zastareo baseline`() {
        // Dug otplaćen a baseline nije osvežen: korisna informacija, ne greška.
        val tb = TriBroja.izracunaj(ukupno = 1, zateceno = 3)
        assertEquals(0, tb.novo)
        assertTrue(tb.baselineZastario)
    }

    // -------- mernaTacka: tri agregata --------

    @Test
    fun `mernaTacka sa baseline-om - ukupno, zateceno, novo`() {
        val ukupan = izvestaj(Pravila.STANJE to 1, Pravila.NESTABILAN to 1, Pravila.HARDKOD to 2, tudjih = 5)
        val baseline = izvestaj(Pravila.HARDKOD to 2)
        val norm = Normalizacija(brojLinija = 2000, kloc = 2.0, brojComposable = 8)

        val t = Metrika.mernaTacka("p", "c", "2026-06-19", ukupan, baseline, norm, Ponderi.PODRAZUMEVANI)

        assertTrue(t.baselinePrisutan)
        val hard = t.poPravilu.getValue(Pravila.HARDKOD)
        assertEquals(2, hard.ukupno); assertEquals(2, hard.zateceno); assertEquals(0, hard.novo)
        val nest = t.poPravilu.getValue(Pravila.NESTABILAN)
        assertEquals(1, nest.ukupno); assertEquals(0, nest.zateceno); assertEquals(1, nest.novo)
        // zbir: ukupno 1*3+1*1.4+2*1=6.4; zatečeno 2*1=2.0; novo 1*3+1*1.4=4.4
        assertEquals(6.4, t.ponderisaniZbir.ukupno, 1e-9)
        assertEquals(2.0, t.ponderisaniZbir.zateceno, 1e-9)
        assertEquals(4.4, t.ponderisaniZbir.novo, 1e-9)
        assertEquals(2.2, t.dugPoKloc!!.novo, 1e-9)   // 4.4 / 2.0
        assertEquals(5, t.ukupnoTudjih)
    }

    @Test
    fun `mernaTacka bez baseline-a - zateceno 0, novo = ukupno`() {
        val ukupan = izvestaj(Pravila.NESTABILAN to 31, Pravila.HARDKOD to 2, tudjih = 47)
        val norm = Normalizacija(brojLinija = 6978, kloc = 6.978, brojComposable = 149)

        val t = Metrika.mernaTacka("jetsnack", "bc18", "2026-06-19", ukupan, null, norm, Ponderi.PODRAZUMEVANI)

        assertFalse(t.baselinePrisutan)
        val nest = t.poPravilu.getValue(Pravila.NESTABILAN)
        assertEquals(31, nest.ukupno); assertEquals(0, nest.zateceno); assertEquals(31, nest.novo)
        // ukupno == novo kad nema baseline-a
        assertEquals(t.ponderisaniZbir.ukupno, t.ponderisaniZbir.novo, 1e-9)
        assertEquals(0.0, t.ponderisaniZbir.zateceno, 1e-9)
    }

    @Test
    fun `mernaTacka - deljenje nulom daje normalizovano null`() {
        val ukupan = izvestaj(Pravila.HARDKOD to 1)
        val norm = Normalizacija(brojLinija = 0, kloc = 0.0, brojComposable = 0)

        val t = Metrika.mernaTacka("prazan", "c", "2026-01-01", ukupan, null, norm, Ponderi.PODRAZUMEVANI)

        assertNull(t.dugPoKloc)
        assertNull(t.dugPoComposable)
        assertEquals(1.0, t.ponderisaniZbir.ukupno, 1e-9)
    }
}
