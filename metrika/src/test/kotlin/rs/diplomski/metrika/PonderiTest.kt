package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** TEST-KORPUS za pondere (test-first): vrednost, podrazumevani, učitavanje. */
class PonderiTest {

    @Test
    fun `ponder je proizvod uticaja i pouzdanosti`() {
        assertEquals(3.0, Ponder(3, 1.0).vrednost, 1e-9)
        assertEquals(1.4, Ponder(2, 0.7).vrednost, 1e-9)
    }

    @Test
    fun `podrazumevani ponderi po merilu`() {
        val p = Ponderi.PODRAZUMEVANI
        assertEquals(3.0, p.getValue(Pravila.STANJE).vrednost, 1e-9)
        assertEquals(1.4, p.getValue(Pravila.ALOKACIJA).vrednost, 1e-9)
        assertEquals(1.4, p.getValue(Pravila.NESTABILAN).vrednost, 1e-9)
        assertEquals(1.0, p.getValue(Pravila.HARDKOD).vrednost, 1e-9)
    }

    @Test
    fun `izTeksta menja navedene, cuva ostale, preskace komentare`() {
        val tekst = """
            # promeni samo hardkod
            HardkodovaniString = 2 x 1.0

            StanjeBezRemember = 1 x 0.4
        """.trimIndent()
        val p = Ponderi.izTeksta(tekst)

        assertEquals(2.0, p.getValue(Pravila.HARDKOD).vrednost, 1e-9)   // promenjen
        assertEquals(0.4, p.getValue(Pravila.STANJE).vrednost, 1e-9)    // promenjen
        assertEquals(1.4, p.getValue(Pravila.NESTABILAN).vrednost, 1e-9) // ostao
    }

    @Test
    fun `izTeksta - neispravan red pada MetrikaGreskom`() {
        assertThrows(MetrikaGreska::class.java) { Ponderi.izTeksta("bez znaka jednakosti") }
        assertThrows(MetrikaGreska::class.java) { Ponderi.izTeksta("Pravilo = 2") }
    }
}
