package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/** TEST-KORPUS za izlazni zapis (test-first): CSV, JSON, projekat, null, dopisivanje. */
class ZapisTest {

    private val tacka = MernaTacka(
        projekat = "jetsnack",
        commitHash = "abc123",
        datum = "2026-07-15",
        kloc = 2.0,
        brojComposable = 8,
        poPravilu = mapOf(
            Pravila.STANJE to 1,
            Pravila.ALOKACIJA to 0,
            Pravila.NESTABILAN to 1,
            Pravila.HARDKOD to 2,
        ),
        ponderisaniZbir = 6.4,
        dugPoKloc = 3.2,
        dugPoComposable = 0.8,
        ukupnoTudjih = 2,
    )

    @Test
    fun `csv zaglavlje ima projekat prvo i pravila u fiksnom redosledu`() {
        assertEquals(
            "projekat,commit_hash,datum,kloc,broj_composable," +
                "StanjeBezRemember,SkupaAlokacijaBezRemember,NestabilanTipParametra,HardkodovaniString," +
                "ponderisani_zbir,dug_po_kloc,dug_po_composable,ukupno_tudjih_upozorenja",
            Zapis.csvZaglavlje(),
        )
    }

    @Test
    fun `csv red - projekat prvo, decimale sa tackom`() {
        assertEquals(
            "jetsnack,abc123,2026-07-15,2.0000,8,1,0,1,2,6.4000,3.2000,0.8000,2",
            Zapis.csvRed(tacka),
        )
    }

    @Test
    fun `csv red - null odnos je PRAZNO polje`() {
        val t = tacka.copy(dugPoKloc = null, dugPoComposable = null)
        // ...,ponderisani_zbir,,,ukupno... -> dva prazna polja između zbira i tuđih.
        assertEquals(
            "jetsnack,abc123,2026-07-15,2.0000,8,1,0,1,2,6.4000,,,2",
            Zapis.csvRed(t),
        )
    }

    @Test
    fun `json red - projekat i null`() {
        val t = tacka.copy(dugPoKloc = null, dugPoComposable = null)
        val json = Zapis.jsonRed(t)
        assertTrue(json.contains("\"projekat\":\"jetsnack\""))
        assertTrue(json.contains("\"dug_po_kloc\":null"))
        assertTrue(json.contains("\"dug_po_composable\":null"))
    }

    @Test
    fun `json red - pun sadrzaj kad odnosi postoje`() {
        assertEquals(
            "{\"projekat\":\"jetsnack\",\"commit_hash\":\"abc123\",\"datum\":\"2026-07-15\"," +
                "\"kloc\":2.0000,\"broj_composable\":8," +
                "\"po_pravilu\":{\"StanjeBezRemember\":1,\"SkupaAlokacijaBezRemember\":0," +
                "\"NestabilanTipParametra\":1,\"HardkodovaniString\":2}," +
                "\"ponderisani_zbir\":6.4000,\"dug_po_kloc\":3.2000,\"dug_po_composable\":0.8000," +
                "\"ukupno_tudjih_upozorenja\":2}",
            Zapis.jsonRed(tacka),
        )
    }

    @Test
    fun `ucitavanje - prazno polje postaje null`() {
        val csv = Zapis.csvZaglavlje() + "\n" +
            Zapis.csvRed(tacka.copy(dugPoKloc = null, dugPoComposable = null)) + "\n"
        val ucitane = Zapis.ucitajCsv(csv)
        assertEquals(1, ucitane.size)
        assertEquals(null, ucitane[0].dugPoKloc)
        assertEquals(null, ucitane[0].dugPoComposable)
    }

    @Test
    fun `dopisivanje - zaglavlje jednom, pa redovi`() {
        val fajl = Files.createTempFile("metrika-test", ".csv")
        Files.deleteIfExists(fajl)
        try {
            Zapis.dopisiCsv(fajl, tacka)
            Zapis.dopisiCsv(fajl, tacka.copy(commitHash = "def456"))

            val linije = Files.readAllLines(fajl)
            assertEquals(3, linije.size)
            assertEquals(Zapis.csvZaglavlje(), linije[0])
            assertEquals("jetsnack", linije[1].substringBefore(",")) // projekat je prva kolona
        } finally {
            Files.deleteIfExists(fajl)
        }
    }
}
