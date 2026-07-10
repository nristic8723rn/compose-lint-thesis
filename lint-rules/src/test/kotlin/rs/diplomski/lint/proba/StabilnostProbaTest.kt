package rs.diplomski.lint.proba

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

/**
 * Spike testovi za fazu 2c (nosivi dokaz za docs/2c-prototip.md).
 *
 * Cilj NIJE gotovo pravilo, nego OSMOTRENI odgovori na 3 pitanja. Svaki
 * expectContains je jedna dokazana činjenica o tome šta lint razrešava.
 * Ako bi neka tvrdnja bila neistinita, odgovarajući test bi pao - zato su
 * ovo dokazi, ne mišljenja.
 *
 * testModes(DEFAULT): iskључujemo dodatne test-režime (npr. JVM_OVERLOADS
 * koji ubacuje sintetički parametar) da bi izlaz bio deterministički.
 */
class StabilnostProbaTest {

    private fun proba() = lint()
        .issues(StabilnostProbaDetector.PROBA)
        .allowMissingSdk()
        .testModes(TestMode.DEFAULT)

    // runtime stub sa @Composable, @Stable, @Immutable (izvor -> isti "modul").
    private val runtime = kotlin(
        """
        package androidx.compose.runtime

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
        annotation class Composable

        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
        annotation class Stable

        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
        annotation class Immutable
        """,
    ).indented()

    private val layout = kotlin(
        """
        package androidx.compose.foundation.layout

        import androidx.compose.runtime.Composable

        @Composable
        fun Column(content: @Composable () -> Unit) {}
        """,
    ).indented()

    // =================================================================
    // PITANJE 1: razrešeni tipovi parametara @Composable funkcije,
    //            signali stabilnosti (a) @Stable/@Immutable, (b) kolekcija,
    //            (c) data klasa sa isključivo val primitivnim/String poljima.
    //            Tipovi su IZVORNI (isti modul).
    // =================================================================
    @Test
    fun `pitanje1 signali stabilnosti za izvorne tipove`() {
        proba().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.Stable
                import androidx.compose.runtime.Immutable

                @Stable class StabilanTip
                @Immutable class NepromenljivTip
                data class DataPrim(val a: Int, val b: String)
                data class DataNePrim(val x: StabilanTip)
                class Obican

                @Composable
                fun Ekran(
                    s: StabilanTip,
                    n: NepromenljivTip,
                    lista: List<String>,
                    mapa: Map<String, Int>,
                    dp: DataPrim,
                    dn: DataNePrim,
                    o: Obican,
                ) {
                }
                """,
            ).indented(),
        ).run()
            // (a) @Stable / @Immutable se čitaju sa razrešenog tipa.
            .expectContains("PARAM name=s fqn=test.StabilanTip STABLE=true")
            .expectContains("PARAM name=n fqn=test.NepromenljivTip STABLE=false IMMUTABLE=true")
            // (b) List/Map se prepoznaju kao kolekcije (pažnja: FQN je java.util.*).
            .expectContains("PARAM name=lista fqn=java.util.List STABLE=false IMMUTABLE=false COLLECTION=true")
            .expectContains("PARAM name=mapa fqn=java.util.Map STABLE=false IMMUTABLE=false COLLECTION=true")
            // (c) data klasa: sva val primitivna/String polja -> ALLVALPRIM=true;
            //     ne-primitivno polje -> ALLVALPRIM=false; oba su DATA=true.
            .expectContains("PARAM name=dp fqn=test.DataPrim STABLE=false IMMUTABLE=false COLLECTION=false DATA=true ALLVALPRIM=true")
            .expectContains("PARAM name=dn fqn=test.DataNePrim STABLE=false IMMUTABLE=false COLLECTION=false DATA=true ALLVALPRIM=false")
            // Obična klasa: nijedan signal.
            .expectContains("PARAM name=o fqn=test.Obican STABLE=false IMMUTABLE=false COLLECTION=false DATA=false ALLVALPRIM=false")
    }

    // =================================================================
    // PITANJE 2: iz poziva sa lambdom, razrešiti DEKLARISANI tip parametra
    //            i videti @Composable na tipu (odlučuje lambda kompromis
    //            pravila 1). Composable lambda mora biti razlučiva od
    //            ne-composable.
    // =================================================================
    @Test
    fun `pitanje2 composable lambda razluciva od ne-composable`() {
        proba().files(
            runtime,
            layout,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.layout.Column

                fun naKlik(akcija: () -> Unit) {}

                @Composable
                fun Ekran() {
                    Column {
                        val x = 1
                    }
                    naKlik {
                        val y = 2
                    }
                }
                """,
            ).indented(),
        ).run()
            // @Composable je vidljiv na TIPU parametra (ne na parametru).
            .expectContains("LAMBDA poziv=Column param=content paramAnno=false typeAnno=true")
            // Ne-composable lambda: typeAnno=false -> razlučivo.
            .expectContains("LAMBDA poziv=naKlik param=akcija paramAnno=false typeAnno=false")
    }

    // =================================================================
    // PITANJE 3: obim - koji signali rade za tipove iz DRUGIH modula
    //            (samo potpisi/bytecode, bez izvora). List/Map/Pair/Observable
    //            nemaju izvor u testu; lint ih razrešava iz kotlin-stdlib/JDK.
    // =================================================================
    @Test
    fun `pitanje3 signali za bytecode tipove iz drugih modula`() {
        proba().files(
            runtime,
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import java.util.Observable

                @Composable
                fun Ekran(
                    lista: List<String>,
                    par: Pair<Int, String>,
                    depr: Observable,
                ) {
                }
                """,
            ).indented(),
        ).run()
            // (b) kolekcija: radi cross-modul (java.util.List iz JDK bytecode-a).
            .expectContains("PARAM name=lista fqn=java.util.List STABLE=false IMMUTABLE=false COLLECTION=true")
            // (c) data detekcija (copy/componentN) radi iz bytecode-a (kotlin.Pair);
            //     ALLVALPRIM=false jer su polja generička (erasovana u Object).
            .expectContains("PARAM name=par fqn=kotlin.Pair STABLE=false IMMUTABLE=false COLLECTION=false DATA=true ALLVALPRIM=false")
            // (a) MEHANIZAM: anotacija na klasi se čita iz bytecode-a. Observable
            //     nosi @Deprecated (RUNTIME) -> vidljivo. @Stable/@Immutable su
            //     BINARY retention -> po istom mehanizmu vidljivi cross-modul.
            .expectContains("PARAM name=depr fqn=java.util.Observable STABLE=false IMMUTABLE=false COLLECTION=false DATA=false ALLVALPRIM=false ANNOS=[java.lang.Deprecated]")
    }
}
