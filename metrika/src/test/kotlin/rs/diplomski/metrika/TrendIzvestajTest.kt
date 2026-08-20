package rs.diplomski.metrika

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TEST-KORPUS za trend (faza 3c): ukupan vs novi, po projektu, trojni prikaz. */
class TrendIzvestajTest {

    private fun tacka(
        projekat: String,
        hash: String,
        datum: String,
        baseline: Boolean = true,
        dugKloc: TriDouble? = TriDouble(6.4, 2.0, 4.4),
    ) = MernaTacka(
        projekat = projekat,
        commitHash = hash,
        datum = datum,
        kloc = 2.0,
        brojComposable = 8,
        poPravilu = mapOf(
            Pravila.STANJE to TriBroja.izracunaj(1, 0),
            Pravila.ALOKACIJA to TriBroja.izracunaj(0, 0),
            Pravila.NESTABILAN to TriBroja.izracunaj(1, 0),
            Pravila.HARDKOD to TriBroja.izracunaj(2, 2),
        ),
        baselinePrisutan = baseline,
        ponderisaniZbir = TriDouble(6.4, 2.0, 4.4),
        dugPoKloc = dugKloc,
        dugPoComposable = TriDouble(0.8, 0.25, 0.55),
        ukupnoTudjih = 5,
    )

    @Test
    fun `primarni grafikon ima ukupan i novi dug kao dve serije`() {
        val html = TrendIzvestaj.generisi(listOf(tacka("jetsnack", "aaa", "2026-01-01")), "2026-07-15")
        assertTrue(html.contains("Ukupan dug / KLOC"))
        assertTrue(html.contains("Novi dug / KLOC"))
    }

    @Test
    fun `zasebna sekcija po projektu i trojni prikaz u tabeli`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01"), tacka("sample-app", "bbb", "2026-02-01")),
            "2026-07-15",
        )
        assertTrue(html.contains("Projekat: jetsnack"))
        assertTrue(html.contains("Projekat: sample-app"))
        // Trojni prikaz u ćeliji pravila: "ukupno / zatečeno / novo" (npr. HARDKOD 2/2/0).
        assertTrue(html.contains("2 / 2 / 0"))
    }

    @Test
    fun `baseline nije uveden - info poruka i zatečeno 0`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01", baseline = false)),
            "2026-07-15",
        )
        assertTrue(html.contains("Baseline nije uveden"))
    }

    @Test
    fun `upozorenje za manje od 3 tacke`() {
        val html = TrendIzvestaj.generisi(listOf(tacka("jetsnack", "aaa", "2026-01-01")), "2026-07-15")
        assertTrue(html.contains("Nedovoljno tačaka za trend"))
    }

    @Test
    fun `null normalizovano - crtica u tabeli`() {
        val html = TrendIzvestaj.generisi(
            listOf(tacka("jetsnack", "aaa", "2026-01-01", dugKloc = null)),
            "2026-07-15",
        )
        assertTrue(html.contains("—"))
    }

    @Test
    fun `offline - nema spoljnih URL-ova`() {
        val html = TrendIzvestaj.generisi(listOf(tacka("jetsnack", "aaa", "2026-01-01")), "2026-07-15")
        assertFalse(html.contains("http"))
    }
}
