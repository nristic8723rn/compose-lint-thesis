package rs.diplomski.metrika

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

/**
 * TEST-KORPUS za normalizatore (test-first). Čisti brojači se testiraju nad
 * stringovima; obilazak stabla nad malim fixture stablom sa POZNATIM tačnim
 * brojevima (main: 18 nepraznih linija, 2 @Composable; test izvor se isključuje).
 */
class NormalizatoriTest {

    private fun korenStabla(): Path =
        Paths.get(NormalizatoriTest::class.java.getResource("/izvorno-stablo")!!.toURI())

    // -------- čisti brojači --------

    @Test
    fun `neprazne linije - prazne se ne broje`() {
        val tekst = "a\n\n  \nb\n"
        assertEquals(2, Normalizatori.prebrojNepneprazneLinije(tekst))
    }

    @Test
    fun `composable - realne anotacije se broje`() {
        val tekst = "@Composable\nfun A() {}\n@Composable\nfun B() {}"
        assertEquals(2, Normalizatori.prebrojComposable(tekst))
    }

    @Test
    fun `composable - linijski komentar se preskace`() {
        val tekst = "// @Composable u komentaru\n@Composable\nfun A() {}"
        assertEquals(1, Normalizatori.prebrojComposable(tekst))
    }

    @Test
    fun `composable - blok komentar i import se preskacu`() {
        val tekst = """
            import androidx.compose.runtime.Composable
            /*
              @Composable u blok komentaru
            */
            @Composable
            fun A() {}
        """.trimIndent()
        assertEquals(1, Normalizatori.prebrojComposable(tekst))
    }

    // -------- isključivanje putanja --------

    @Test
    fun `iskljucuju se build, generated i test izvori`() {
        assertTrue(Normalizatori.jePutanjaIskljucena("app/build/generated/Gen.kt"))
        assertTrue(Normalizatori.jePutanjaIskljucena("app/build/tmp/X.kt"))
        assertTrue(Normalizatori.jePutanjaIskljucena("lib/generated/Y.kt"))
        assertTrue(Normalizatori.jePutanjaIskljucena("app/src/test/kotlin/A.kt"))
        assertTrue(Normalizatori.jePutanjaIskljucena("app/src/androidTest/kotlin/B.kt"))
        assertFalse(Normalizatori.jePutanjaIskljucena("app/src/main/kotlin/C.kt"))
    }

    // -------- obilazak stabla (poznati tačni brojevi) --------

    @Test
    fun `izbrojNad - main fajlovi, test izvor iskljucen`() {
        val n = Normalizatori.izbrojNad(korenStabla())

        // 3 main fajla (Ekran, Model, Comment); EkranTest u src/test je izuzet.
        assertEquals(3, Normalizatori.ktFajlovi(korenStabla()).size)
        // 9 (Ekran) + 2 (Model) + 7 (Comment) = 18 nepraznih linija.
        assertEquals(18, n.brojLinija)
        assertEquals(0.018, n.kloc, 1e-9)
        // 2 (Ekran) + 0 + 0; @Composable u komentaru i u test izvoru se ne broje.
        assertEquals(2, n.brojComposable)
    }
}
