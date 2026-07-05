package rs.diplomski.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * Prvi LintDetectorTest (Faza 1, korak 3).
 *
 * Obrazac koji važi za SVA buduća pravila:
 *  - pozitivan slučaj: kod koji krši pravilo -> očekujemo tačan broj grešaka
 *  - negativan slučaj: čist kod -> očekujemo nula nalaza (expectClean)
 *
 * Test harnisa (lint-tests) podiže pravi lint engine u memoriji i pušta
 * ga na string sa izvornim kodom - dakle testira se stvarno ponašanje
 * pravila, ne mock.
 */
class ZabranjenoDetectorTest {

    @Test
    fun `poziv zabranjeno funkcije prijavljuje se kao greska`() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun zabranjeno() {}

                    fun pozivalac() {
                        zabranjeno()
                    }
                    """,
                ).indented(),
            )
            .issues(ZabranjenoDetector.ISSUE)
            .allowMissingSdk() // JVM modul nema Android SDK - i ne treba mu
            .run()
            .expectErrorCount(1)

    }

    @Test
    fun `cist kod prolazi bez nalaza`() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun dozvoljeno() {}

                    fun pozivalac() {
                        dozvoljeno()
                    }
                    """,
                ).indented(),
            )
            .issues(ZabranjenoDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}