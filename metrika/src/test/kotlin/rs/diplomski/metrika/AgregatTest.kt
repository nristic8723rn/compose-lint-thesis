package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TEST-KORPUS za agregat (test-first). Ključni test za rad: izmena pondera
 * menja agregat na očekivan način (osetljivost metrike na izbor pondera).
 */
class AgregatTest {

    // Nalazi kao u „normalan.xml": STANJE=1, ALOKACIJA=0, NESTABILAN=1, HARDKOD=2.
    private val poPravilu = mapOf(
        Pravila.STANJE to 1,
        Pravila.ALOKACIJA to 0,
        Pravila.NESTABILAN to 1,
        Pravila.HARDKOD to 2,
    )

    @Test
    fun `ponderisani zbir sa podrazumevanim ponderima`() {
        val a = Metrika.agregiraj(poPravilu, Ponderi.PODRAZUMEVANI, kloc = 2.0, brojComposable = 8)
        // 1*3.0 + 0*1.4 + 1*1.4 + 2*1.0 = 6.4
        assertEquals(6.4, a.ponderisaniZbir, 1e-9)
        assertEquals(3.2, a.dugPoKloc, 1e-9)         // 6.4 / 2.0
        assertEquals(0.8, a.dugPoComposable, 1e-9)   // 6.4 / 8
    }

    @Test
    fun `izmena pondera menja agregat na ocekivan nacin`() {
        // Dignemo HardkodovaniString sa 1.0 na 2.0 -> zbir raste za 2*1.0.
        val drugi = Ponderi.izTeksta("HardkodovaniString = 2 x 1.0")
        val a = Metrika.agregiraj(poPravilu, drugi, kloc = 2.0, brojComposable = 8)
        // 3.0 + 0 + 1.4 + 2*2.0 = 8.4
        assertEquals(8.4, a.ponderisaniZbir, 1e-9)
    }

    @Test
    fun `delilac nula - odnos je 0 (otvoreno pitanje, dokumentovano)`() {
        val a = Metrika.agregiraj(poPravilu, Ponderi.PODRAZUMEVANI, kloc = 0.0, brojComposable = 0)
        assertEquals(6.4, a.ponderisaniZbir, 1e-9)
        assertEquals(0.0, a.dugPoKloc, 1e-9)
        assertEquals(0.0, a.dugPoComposable, 1e-9)
    }
}
