package rs.diplomski.lint

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import org.junit.Test

/**
 * TEST-KORPUS za pravilo 1 (korak "testovi pre detektora").
 *
 * Ovi testovi su UGOVOR pravila: pozitivni slučajevi definišu šta se
 * hvata, negativni šta se namerno propušta. Pisani su PRE implementacije
 * i na početku su crveni; implementacija je gotova kad svi pozelene.
 *
 * Svaki test dokumentuje i ZAŠTO je slučaj takav kakav je - ti komentari
 * su sirovina za poglavlje o dizajnu pravila.
 */
class RememberDetectorTest {

    private fun lintZaStanje() = lint()
        .issues(RememberDetector.ISSUE_STANJE)
        .allowMissingSdk()

    private fun lintZaAlokaciju() = lint()
        .issues(RememberDetector.ISSUE_ALOKACIJA)
        .allowMissingSdk()

    // =================================================================
    // PODSLUČAJ A - POZITIVNI (kod koji MORA biti prijavljen)
    // =================================================================

    @Test
    fun `A-poz-1 mutableStateOf direktno u telu composable funkcije`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf

                @Composable
                fun Brojac() {
                    val stanje = mutableStateOf(0) // stanje se gubi na rekompoziciju
                }
                """,
            ).indented(),
        ).run().expectErrorCount(1)
    }

    @Test
    fun `A-poz-2 mutableIntStateOf u telu composable funkcije`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableIntStateOf

                @Composable
                fun Brojac() {
                    val stanje = mutableIntStateOf(0)
                }
                """,
            ).indented(),
        ).run().expectErrorCount(1)
    }

    @Test
    fun `A-poz-3 derivedStateOf van remember bloka`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.derivedStateOf

                @Composable
                fun Prikaz(ulaz: Int) {
                    val izvedeno = derivedStateOf { ulaz * 2 }
                }
                """,
            ).indented(),
        ).run().expectErrorCount(1)
    }

    @Test
    fun `A-poz-4 vise prekrsaja u istoj funkciji - svaki se prijavljuje`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf
                import androidx.compose.runtime.remember

                @Composable
                fun Ekran() {
                    val ok = remember { mutableStateOf(0) } // ispravno, ne prijavljuje se
                    val los1 = mutableStateOf(1)
                    val los2 = mutableStateOf(2)
                }
                """,
            ).indented(),
        ).run().expectErrorCount(2)
    }

    // =================================================================
    // PODSLUČAJ A - NEGATIVNI (kod koji NE SME biti prijavljen)
    // =================================================================

    @Test
    fun `A-neg-1 mutableStateOf unutar remember bloka`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf
                import androidx.compose.runtime.remember

                @Composable
                fun Brojac() {
                    val stanje = remember { mutableStateOf(0) }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `A-neg-2 remember sa kljucem`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf
                import androidx.compose.runtime.remember

                @Composable
                fun Brojac(id: Int) {
                    val stanje = remember(id) { mutableStateOf(0) }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `A-neg-3 rememberSaveable blok`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf
                import androidx.compose.runtime.saveable.rememberSaveable

                @Composable
                fun Brojac() {
                    val stanje = rememberSaveable { mutableStateOf(0) }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `A-neg-4 mutableStateOf u obicnoj ne-composable funkciji`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.mutableStateOf

                class DrzacStanja {
                    // Uobičajen i ISPRAVAN obrazac: stanje u klasi (ViewModel),
                    // van composable konteksta - remember tu nema smisla.
                    val stanje = mutableStateOf(0)

                    fun napravi() = mutableStateOf(1)
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `A-neg-5 poziv unutar ne-composable lambde (onClick)`() {
        // Svesni kompromis (dizajn, tačka 2 negativnih obrazaca):
        // lambda se izvršava na događaj, ne na rekompoziciju.
        // Prvom verzijom pravila NE ulazimo u razlikovanje composable
        // od ne-composable lambdi -> unutar lambde ćutimo.
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.mutableStateOf

                fun naKlik(akcija: () -> Unit) {}

                @Composable
                fun Dugme() {
                    naKlik {
                        val privremeno = mutableStateOf(0)
                    }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `A-neg-6 poziv unutar LaunchedEffect bloka`() {
        lintZaStanje().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.LaunchedEffect
                import androidx.compose.runtime.mutableStateOf

                @Composable
                fun Ekran() {
                    LaunchedEffect(Unit) {
                        val lokalno = mutableStateOf(0)
                    }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    // =================================================================
    // PODSLUČAJ B - POZITIVNI
    // =================================================================

    @Test
    fun `B-poz-1 Regex konstruisan u telu composable funkcije`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                @Composable
                fun Validator(unos: String) {
                    val obrazac = Regex("[a-z]+") // kompajlira se na svaku rekompoziciju
                    val validno = obrazac.matches(unos)
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `B-poz-2 SimpleDateFormat u telu composable funkcije`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import java.text.SimpleDateFormat

                @Composable
                fun Datum() {
                    val format = SimpleDateFormat("dd.MM.yyyy")
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    // =================================================================
    // PODSLUČAJ B - NEGATIVNI
    // =================================================================

    @Test
    fun `B-neg-1 Regex unutar remember bloka`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                @Composable
                fun Validator() {
                    val obrazac = remember { Regex("[a-z]+") }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `B-neg-2 Regex u obicnoj ne-composable funkciji`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                fun proveri(unos: String): Boolean {
                    return Regex("[a-z]+").matches(unos)
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `B-neg-3 jeftin tip u telu composable funkcije se ne prijavljuje`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                data class Model(val ime: String)

                @Composable
                fun Prikaz() {
                    // Nije na listi skupih tipova -> pravilo ćuti.
                    // (Da li je OVO trebalo zapamtiti je pitanje za
                    // pravilo 2 o stabilnosti, ne za ovo pravilo.)
                    val model = Model("test")
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `B-neg-4 skup tip unutar SideEffect bloka`() {
        lintZaAlokaciju().files(
            *ComposeStubovi.SVI,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.SideEffect

                @Composable
                fun Ekran() {
                    SideEffect {
                        val obrazac = Regex("[a-z]+")
                    }
                }
                """,
            ).indented(),
        ).run().expectClean()
    }
}
