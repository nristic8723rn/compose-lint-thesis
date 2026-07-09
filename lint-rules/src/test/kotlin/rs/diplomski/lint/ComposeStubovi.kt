package rs.diplomski.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

/**
 * Compose STUBOVI za testove.
 *
 * Ključna činjenica koju vredi zapisati u rad: lint test harnisa NEMA
 * Compose na classpath-u. Da bi test kod sa @Composable / remember /
 * mutableStateOf uopšte kompajlirao i RAZREŠIO tipove (a rezolucija
 * nam treba za pravilo), testovima prilažemo minimalne deklaracije
 * stvarnih Compose API-ja. Bitni su POTPISI, ne implementacije.
 */
object ComposeStubovi {

    val RUNTIME: TestFile = kotlin(
        """
        package androidx.compose.runtime

        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER,
        )
        annotation class Composable

        interface State<out T> { val value: T }
        interface MutableState<T> : State<T> { override var value: T }

        fun <T> mutableStateOf(value: T): MutableState<T> = throw NotImplementedError()
        fun mutableIntStateOf(value: Int): MutableState<Int> = throw NotImplementedError()
        fun <T> mutableStateListOf(vararg elements: T): MutableList<T> = throw NotImplementedError()
        fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> = throw NotImplementedError()
        fun <T> derivedStateOf(calculation: () -> T): State<T> = throw NotImplementedError()

        @Composable
        fun <T> remember(calculation: () -> T): T = throw NotImplementedError()

        @Composable
        fun <T> remember(key1: Any?, calculation: () -> T): T = throw NotImplementedError()

        @Composable
        fun LaunchedEffect(key1: Any?, block: suspend () -> Unit) {
        }

        @Composable
        fun SideEffect(effect: () -> Unit) {
        }
        """,
    ).indented()

    val SAVEABLE: TestFile = kotlin(
        """
        package androidx.compose.runtime.saveable

        import androidx.compose.runtime.Composable

        @Composable
        fun <T : Any> rememberSaveable(vararg inputs: Any?, init: () -> T): T =
            throw NotImplementedError()
        """,
    ).indented()

    /** Minimalni UI stub - Composable koji prima composable lambdu (kao Column). */
    val UI: TestFile = kotlin(
        """
        package androidx.compose.foundation.layout

        import androidx.compose.runtime.Composable

        @Composable
        fun Column(content: @Composable () -> Unit) {
        }
        """,
    ).indented()

    /** Svi stubovi zajedno - zgodno za .files(...) pozive. */
    val SVI: Array<TestFile> = arrayOf(RUNTIME, SAVEABLE, UI)
}
