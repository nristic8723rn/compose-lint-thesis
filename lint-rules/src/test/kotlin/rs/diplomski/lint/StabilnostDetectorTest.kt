package rs.diplomski.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

/**
 * TEST-KORPUS za pravilo 2 v1 (nestabilan tip parametra @Composable funkcije).
 *
 * Pisano test-first: pozitivni definišu šta hvatamo (kolekcijski interfejsi
 * bez stabilnosne garancije), negativni šta svesno propuštamo. Svaki test
 * nosi komentar ZAŠTO - sirovina za rad.
 *
 * testModes(DEFAULT): po lekciji iz spike-a faze 2c (druge test-režime, npr.
 * JVM_OVERLOADS, isključujemo da izlaz bude deterministički).
 */
class StabilnostDetectorTest {

    private fun stabilnost() = lint()
        .issues(StabilnostDetector.ISSUE)
        .allowMissingSdk()
        .testModes(TestMode.DEFAULT)

    // runtime stub sa @Composable, @Stable, @Immutable.
    private val runtime = kotlin(
        """
        package androidx.compose.runtime

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
        annotation class Composable

        @Target(AnnotationTarget.CLASS)
        annotation class Stable

        @Target(AnnotationTarget.CLASS)
        annotation class Immutable
        """,
    ).indented()

    // =================================================================
    // POZITIVNI (kod koji MORA biti prijavljen)
    // =================================================================

    @Test
    fun `poz-1 List parametar bez stabilnosne garancije`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                @Composable
                fun Lista(stavke: List<String>) {}
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `poz-2 Set parametar bez stabilnosne garancije`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                @Composable
                fun Skup(stavke: Set<Int>) {}
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    @Test
    fun `poz-3 Map parametar bez stabilnosne garancije`() {
        // Map ne nasleđuje Collection - poseban put kroz java.util.Map.
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                @Composable
                fun Mapa(par: Map<String, Int>) {}
                """,
            ).indented(),
        ).run().expectWarningCount(1)
    }

    // =================================================================
    // NEGATIVNI (kod koji NE SME biti prijavljen)
    // =================================================================

    @Test
    fun `neg-1 kolekcijski tip oznacen @Immutable - garancija stabilnosti`() {
        // Gate se testira NE-vakuumno: tip JESTE kolekcija (nasleđuje List),
        // ali @Immutable garantuje stabilnost -> ćutimo. (Model kotlinx
        // immutable kolekcija.)
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.Immutable

                @Immutable
                interface NepromenljivaLista<E> : List<E>

                @Composable
                fun Lista(stavke: NepromenljivaLista<String>) {}
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-2 tip oznacen @Stable - ne prijavljuje se`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.Stable

                @Stable
                interface StabilanSkup<E> : Set<E>

                @Composable
                fun Skup(stavke: StabilanSkup<Int>) {}
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-3 List parametar u NE-composable funkciji - ne prijavljuje se`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                fun obrada(stavke: List<String>): Int = stavke.size
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-4 konkretna klasa (nije kolekcija) - ne prijavljuje se`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                class Model(val ime: String)

                @Composable
                fun Prikaz(model: Model) {}
                """,
            ).indented(),
        ).run().expectClean()
    }

    @Test
    fun `neg-5 primitivni i String parametri - ne prijavljuju se`() {
        stabilnost().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable

                @Composable
                fun Kartica(naslov: String, broj: Int, aktivan: Boolean) {}
                """,
            ).indented(),
        ).run().expectClean()
    }
}
