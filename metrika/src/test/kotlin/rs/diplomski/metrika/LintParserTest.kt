package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEST-KORPUS za parser (test-first). Fixtures su u src/test/resources/fixtures.
 * Ugovor: filtriramo SAMO naša 4 ID-a; tuđe brojimo kao kontekst; malformiran
 * XML pada jasnom greškom, ne tiho.
 */
class LintParserTest {

    private fun ucitaj(ime: String): String =
        LintParserTest::class.java.getResource("/fixtures/$ime")!!.readText()

    @Test
    fun `normalan izvestaj - filtrira nasa cetiri, broji tudja`() {
        val r = LintParser.parsiraj(ucitaj("normalan.xml"))

        assertEquals(4, r.nasiNalazi.size)
        assertEquals(2, r.ukupnoTudjih) // MissingApplicationIcon + UnrememberedMutableState

        val po = r.poPravilu()
        assertEquals(1, po[Pravila.STANJE])
        assertEquals(0, po[Pravila.ALOKACIJA])
        assertEquals(1, po[Pravila.NESTABILAN])
        assertEquals(2, po[Pravila.HARDKOD])
    }

    @Test
    fun `normalan izvestaj - polja nalaza se citaju tacno`() {
        val r = LintParser.parsiraj(ucitaj("normalan.xml"))
        val stanje = r.nasiNalazi.first { it.idPravila == Pravila.STANJE }

        assertEquals("Error", stanje.ozbiljnost)
        assertEquals("/proj/A.kt", stanje.fajl)
        assertEquals(10, stanje.linija)
        assertTrue(stanje.poruka.isNotBlank())
    }

    @Test
    fun `prazan izvestaj - nula nalaza, sva cetiri pravila nula`() {
        val r = LintParser.parsiraj(ucitaj("prazan.xml"))

        assertTrue(r.nasiNalazi.isEmpty())
        assertEquals(0, r.ukupnoTudjih)
        assertEquals(Pravila.REDOSLED.associateWith { 0 }, r.poPravilu())
    }

    @Test
    fun `izvestaj bez ijednog naseg nalaza - samo kontekst tudjih`() {
        val r = LintParser.parsiraj(ucitaj("bez-nasih.xml"))

        assertTrue(r.nasiNalazi.isEmpty())
        assertEquals(2, r.ukupnoTudjih)
    }

    @Test
    fun `malformiran XML pada MetrikaGreskom, ne tiho`() {
        assertThrows(MetrikaGreska::class.java) {
            LintParser.parsiraj(ucitaj("malformiran.xml"))
        }
    }
}
