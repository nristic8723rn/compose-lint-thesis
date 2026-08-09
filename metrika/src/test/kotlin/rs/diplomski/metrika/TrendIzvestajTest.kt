package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TEST-KORPUS za trend izveštaj (test-first): projekat, CSV round-trip, sadržaj. */
class TrendIzvestajTest {

    private fun tacka(
        projekat: String,
        hash: String,
        datum: String,
        stanje: Int,
        hardkod: Int,
        dugKloc: Double? = 3.2,
    ) = MernaTacka(
        projekat = projekat,
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
        dugPoKloc = dugKloc,
        dugPoComposable = 0.8,
        ukupnoTudjih = 2,
    )

    @Test
    fun `CSV round-trip cuva projekat`() {
        val t1 = tacka("jetsnack", "aaa", "2026-01-01", 1, 2)
        val t2 = tacka("sample-app", "bbb", "2026-02-01", 0, 3)
        val csv = Zapis.csvZaglavlje() + "\n" + Zapis.csvRed(t1) + "\n" + Zapis.csvRed(t2) + "\n"

        val ucitane = Zapis.ucitajCsv(csv)
        assertEquals("jetsnack", ucitane[0].projekat)
        assertEquals("sample-app", ucitane[1].projekat)
    }

    @Test
    fun `HTML - zasebna sekcija po projektu, nikad spojeno`() {
        val html = TrendIzvestaj.generisi(
            listOf(
                tacka("jetsnack", "aaa", "2026-01-01", 1, 2),
                tacka("sample-app", "bbb", "2026-02-01", 0, 3),
            ),
            datumGenerisanja = "2026-07-15",
        )
        assertTrue(html.startsWith("<!doctype html"))
        assertTrue(html.contains("Projekat: jetsnack"))
        assertTrue(html.contains("Projekat: sample-app"))
        assertTrue(html.contains("Projekata: 2"))
        assertTrue(html.contains("<svg"))
        assertTrue(html.contains("<table"))
        Pravila.REDOSLED.forEach { assertTrue("nedostaje $it", html.contains(it)) }
    }

    @Test
    fun `HTML - upozorenje kad projekat ima manje od 3 tacke`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01", 1, 2)),
            datumGenerisanja = "2026-07-15",
        )
        assertTrue(html.contains("Nedovoljno tačaka za trend"))
    }

    @Test
    fun `HTML - null odnos se u tabeli prikazuje kao crtica`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01", 1, 2, dugKloc = null)),
            datumGenerisanja = "2026-07-15",
        )
        assertTrue(html.contains("—")) // prazna vrednost u tabeli
    }

    @Test
    fun `HTML je offline - nema spoljnih URL-ova`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01", 1, 2)),
            datumGenerisanja = "2026-07-15",
        )
        assertFalse("izvestaj mora raditi offline", html.contains("http"))
    }
}
