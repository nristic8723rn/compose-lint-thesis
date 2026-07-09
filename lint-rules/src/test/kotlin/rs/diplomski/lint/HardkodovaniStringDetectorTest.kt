package rs.diplomski.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * TEST-KORPUS za pravilo 3 (hardkodovani stringovi u Compose UI).
 *
 * Kao i kod pravila 1: pozitivni slučajevi definišu šta se hvata, negativni
 * šta se svesno propušta. Svaki test nosi komentar ZAŠTO - sirovina za rad.
 *
 * Ugovor u jednoj rečenici: prijavi NEPRAZAN inline literal (uklj. šablon i
 * konkatenaciju sa literalom) u `text` argumentu `Text` poziva, osim u
 * @Preview funkcijama; sve ostalo (stringResource, prazan string, testTag,
 * konstante) ćuti.
 */
class HardkodovaniStringDetectorTest {

    private fun lintZaHardkod() = lint()
        .issues(HardkodovaniStringDetector.ISSUE)
        .allowMissingSdk()

    // =================================================================
    // POZITIVNI (kod koji MORA biti prijavljen)
    // =================================================================

    @Test
    fun `poz-1 goli string literal kao imenovani text argument`() {
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                @Composable
                fun Pozdrav() {
                    Text(text = "Zdravo, svete")
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `poz-2 pozicioni string literal (bez imena parametra)`() {
        // Mapiranje argumenata mora da radi i za pozicioni oblik Text("...").
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                @Composable
                fun Pozdrav() {
                    Text("Dobrodošli")
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `poz-3 string sablon sa literalnim delom`() {
        // "Zdravo, ${'$'}ime" ima literalni deo "Zdravo, " -> hardkodovan tekst.
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                @Composable
                fun Pozdrav(ime: String) {
                    Text(text = "Zdravo, ${'$'}ime")
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `poz-4 konkatenacija string literala i promenljive`() {
        // Granični slučaj: "Cena: " + cena -> literalni deo postoji.
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                @Composable
                fun Cena(cena: Int) {
                    Text(text = "Cena: " + cena)
                }
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    // =================================================================
    // NEGATIVNI (kod koji NE SME biti prijavljen)
    // =================================================================

    @Test
    fun `neg-1 stringResource je ispravan lokalizovan nacin`() {
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text
                import androidx.compose.ui.res.stringResource

                @Composable
                fun Pozdrav() {
                    Text(text = stringResource(123))
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-2 prazan string se ne prijavljuje`() {
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                @Composable
                fun Prazno() {
                    Text(text = "")
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-3 hardkod u @Preview funkciji je legitiman`() {
        // Preview je razvojni alat; hardkodovan tekst tu ne ide u produkciju.
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text
                import androidx.compose.ui.tooling.preview.Preview

                @Preview
                @Composable
                fun PregledPozdrava() {
                    Text(text = "Zdravo, svete")
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-4 testTag string nije UI tekst pa se ne prijavljuje`() {
        // Gledamo ISKLJUČIVO text parametar; string u testTag nije UI tekst.
        // Sam text je stringResource, pa ceo poziv mora biti čist.
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.testTag
                import androidx.compose.ui.res.stringResource

                @Composable
                fun Tagovano() {
                    Text(
                        text = stringResource(123),
                        modifier = Modifier.testTag("pozdrav_tag"),
                    )
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-5 konstanta iz objekta se svesno ne hvata (ogranicenje)`() {
        // Dokumentovano ograničenje: referencu na const val NE prijavljujemo -
        // hvatanje bi tražilo evaluaciju konstanti i rizik lažnih upozorenja.
        // Kandidat za "Pravce daljeg rada", u duhu preciznost-pre-potpunosti.
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text

                object Tekstovi {
                    const val NASLOV = "Naslov"
                }

                @Composable
                fun Zaglavlje() {
                    Text(text = Tekstovi.NASLOV)
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-6 string van composable konteksta (nije Text) se ne prijavljuje`() {
        // Peti negativni obrazac iz spec-a: pravilo je vezano ISKLJUČIVO za
        // `text` argument `Text` poziva. Goli string literal u običnoj
        // (ne-composable, ne-UI) funkciji nije naša briga - da rule ne bi
        // degenerisao u "flaguj svaki string literal".
        lintZaHardkod().files(
            *ComposeStubovi.SVI_TEXT,
            kotlin(
                """
                package test

                fun formatiraj(): String {
                    return "obican string van composable konteksta"
                }
                """,
            ).indented(),
        ).run().expectClean()
    }
}
