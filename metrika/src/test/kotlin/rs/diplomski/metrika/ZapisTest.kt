package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/** TEST-KORPUS za izlazni zapis (faza 3c): trojna šema, null, round-trip. */
class ZapisTest {

    private fun tacka(dugKloc: TriDouble? = TriDouble(3.2, 1.0, 2.2)) = MernaTacka(
        projekat = "jetsnack",
        commitHash = "abc123",
        datum = "2026-06-19",
        kloc = 2.0,
        brojComposable = 8,
        poPravilu = mapOf(
            Pravila.STANJE to TriBroja.izracunaj(1, 0),
            Pravila.ALOKACIJA to TriBroja.izracunaj(0, 0),
            Pravila.NESTABILAN to TriBroja.izracunaj(1, 0),
            Pravila.HARDKOD to TriBroja.izracunaj(2, 2),
        ),
        baselinePrisutan = true,
        ponderisaniZbir = TriDouble(6.4, 2.0, 4.4),
        dugPoKloc = dugKloc,
        dugPoComposable = TriDouble(0.8, 0.25, 0.55),
        ukupnoTudjih = 5,
    )

    @Test
    fun `zaglavlje ima trojne kolone i baseline_prisutan`() {
        val z = Zapis.csvZaglavlje()
        assertTrue(z.contains("baseline_prisutan"))
        assertTrue(z.contains("StanjeBezRemember_ukupno"))
        assertTrue(z.contains("HardkodovaniString_zateceno"))
        assertTrue(z.contains("HardkodovaniString_novo"))
        assertTrue(z.contains("ponderisani_zbir_novo"))
        assertTrue(z.contains("dug_po_kloc_zateceno"))
        assertTrue(z.contains("dug_po_composable_novo"))
    }

    @Test
    fun `csv red ima isti broj kolona kao zaglavlje`() {
        assertEquals(Zapis.CSV_KOLONE.size, Zapis.csvRed(tacka()).split(",").size)
    }

    @Test
    fun `round-trip - trojni brojevi po pravilu`() {
        val csv = Zapis.csvZaglavlje() + "\n" + Zapis.csvRed(tacka()) + "\n"
        val u = Zapis.ucitajCsv(csv).single()
        assertEquals(2, u.poPravilu.getValue(Pravila.HARDKOD).ukupno)
        assertEquals(2, u.poPravilu.getValue(Pravila.HARDKOD).zateceno)
        assertEquals(0, u.poPravilu.getValue(Pravila.HARDKOD).novo)
        assertEquals(1, u.poPravilu.getValue(Pravila.NESTABILAN).novo)
        assertTrue(u.baselinePrisutan)
        assertEquals(2.2, u.dugPoKloc!!.novo, 1e-9)
    }

    @Test
    fun `null normalizovana vrednost - prazna polja u CSV, null u JSON`() {
        val t = tacka(dugKloc = null)
        // Tri prazna polja za dug_po_kloc (","+"" x3): proveri kroz round-trip.
        val csv = Zapis.csvZaglavlje() + "\n" + Zapis.csvRed(t) + "\n"
        val u = Zapis.ucitajCsv(csv).single()
        assertNull(u.dugPoKloc)

        val json = Zapis.jsonRed(t)
        assertTrue(json.contains("\"dug_po_kloc\":null"))
        assertTrue(json.contains("\"baseline_prisutan\":true"))
        assertTrue(json.contains("\"HardkodovaniString\":{\"ukupno\":2,\"zateceno\":2,\"novo\":0}"))
    }

    @Test
    fun `dopisivanje - zaglavlje jednom, pa redovi`() {
        val fajl = Files.createTempFile("metrika-3c", ".csv")
        Files.deleteIfExists(fajl)
        try {
            Zapis.dopisiCsv(fajl, tacka())
            Zapis.dopisiCsv(fajl, tacka())
            val linije = Files.readAllLines(fajl)
            assertEquals(3, linije.size)
            assertEquals(Zapis.csvZaglavlje(), linije[0])
        } finally {
            Files.deleteIfExists(fajl)
        }
    }
}
