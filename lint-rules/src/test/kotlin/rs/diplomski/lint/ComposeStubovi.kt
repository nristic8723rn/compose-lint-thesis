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

    /**
     * Stubovi za pravilo 3 (hardkodovani stringovi u Compose UI).
     *
     * Isti razlog kao i ostali stubovi: test harnisa nema Compose, a nama
     * treba rezolucija da bismo `Text` razlikovali od tuđe istoimene
     * funkcije, i da bi negativni obrasci (stringResource, testTag) uopšte
     * kompajlirali. Bitni su POTPISI i PAKETI, ne implementacije.
     */

    /** androidx.compose.ui.Modifier - potreban za testTag negativni obrazac. */
    val MODIFIER: TestFile = kotlin(
        """
        package androidx.compose.ui

        abstract class Modifier {
            companion object : Modifier()
        }
        """,
    ).indented()

    /** Modifier.testTag - ne-UI string koji pravilo NE sme da prijavi. */
    val TEST_TAG: TestFile = kotlin(
        """
        package androidx.compose.ui.platform

        import androidx.compose.ui.Modifier

        fun Modifier.testTag(tag: String): Modifier = this
        """,
    ).indented()

    /** stringResource - ispravan (lokalizovan) način, negativni obrazac. */
    val STRING_RESOURCE: TestFile = kotlin(
        """
        package androidx.compose.ui.res

        import androidx.compose.runtime.Composable

        @Composable
        fun stringResource(id: Int): String = ""
        """,
    ).indented()

    /** @Preview - hardkod u preview funkciji je legitiman, negativni obrazac. */
    val PREVIEW: TestFile = kotlin(
        """
        package androidx.compose.ui.tooling.preview

        annotation class Preview
        """,
    ).indented()

    /** material3.Text - meta funkcije koju pravilo 3 gađa. */
    val MATERIAL3_TEXT: TestFile = kotlin(
        """
        package androidx.compose.material3

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier

        @Composable
        fun Text(text: String, modifier: Modifier = Modifier) {
        }
        """,
    ).indented()

    /** Svi stubovi zajedno - zgodno za .files(...) pozive. */
    val SVI: Array<TestFile> = arrayOf(RUNTIME, SAVEABLE, UI)

    /** Prošireni skup stubova za pravilo 3 (Text + okruženje). */
    val SVI_TEXT: Array<TestFile> = arrayOf(
        RUNTIME, SAVEABLE, UI,
        MODIFIER, TEST_TAG, STRING_RESOURCE, PREVIEW, MATERIAL3_TEXT,
    )
}
