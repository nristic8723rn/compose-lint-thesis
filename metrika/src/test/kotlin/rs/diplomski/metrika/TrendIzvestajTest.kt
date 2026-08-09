package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TEST-KORPUS za trend izveštaj (test-first): CSV round-trip + sadržaj HTML-a. */
class TrendIzvestajTest {

    private fun tacka(hash: String, datum: String, stanje: Int, hardkod: Int) = MernaTacka(
        commitHash = hash,
        datum = datum,
        kloc = 2.0,
        brojComposable = 8,
        poPravilu = mapOf(
            Pravila.STANJE to stanje,
            Pravila.ALOKACIJA to 0,
            Pravila.NESTABILAN to 1,
            Pravila.HARDKOD to hardkod,
        ),
        ponderisaniZbir = 6.4,
        dugPoKloc = 3.2,
        dugPoComposable = 0.8,
        ukupnoTudjih = 2,
    )

    @Test
    fun `CSV round-trip - zapis pa ucitavanje vraca iste tacke`() {
        val t1 = tacka("aaa", "2026-01-01", 1, 2)
        val t2 = tacka("bbb", "2026-02-01", 0, 3)
        val csv = Zapis.csvZaglavlje() + "\n" + Zapis.csvRed(t1) + "\n" + Zapis.csvRed(t2) + "\n"

        val ucitane = Zapis.ucitajCsv(csv)
        assertEquals(2, ucitane.size)
        assertEquals("2026-02-01", ucitane[1].datum)
        assertEquals(1, ucitane[0].poPravilu[Pravila.STANJE])
        assertEquals(3, ucitane[1].poPravilu[Pravila.HARDKOD])
        assertEquals(3.2, ucitane[0].dugPoKloc, 1e-9)
    }

    @Test
    fun `HTML sadrzi oba grafikona, razbijanje po pravilu, tabelu i legendu`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("aaa", "2026-01-01", 1, 2), tacka("bbb", "2026-02-01", 0, 3)),
            datumGenerisanja = "2026-07-15",
        )

        assertTrue(html.startsWith("<!doctype html"))
        assertTrue(html.contains("<svg"))
        assertTrue(html.contains("Generisano: 2026-07-15"))
        assertTrue(html.contains("Dug po KLOC"))
        assertTrue(html.contains("Dug po @Composable"))
        assertTrue(html.contains("<table"))
        // Razbijanje po pravilu: sva 4 ID-a se pojavljuju (agregat nije sam).
        Pravila.REDOSLED.forEach { assertTrue("nedostaje $it", html.contains(it)) }
    }

    @Test
    fun `HTML je offline - nema spoljnih URL-ova`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("aaa", "2026-01-01", 1, 2)),
            datumGenerisanja = "2026-07-15",
        )
        assertFalse("izvestaj mora raditi offline", html.contains("http"))
    }
}
