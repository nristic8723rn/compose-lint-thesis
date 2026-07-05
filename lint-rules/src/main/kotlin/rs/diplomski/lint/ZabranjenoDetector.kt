package rs.diplomski.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * PROBNO PRAVILO (Faza 1, korak 2).
 *
 * Namerno trivijalno: prijavljuje svaki poziv funkcije sa imenom
 * "zabranjeno". Cilj nije korisnost, nego dokaz da ceo lanac radi:
 * registracija pravila -> IDE podvlačenje -> pad lint zadatka -> pad CI-ja.
 *
 * Ujedno je i šablon za prava pravila iz Faze 2: ista struktura
 * (Detector + SourceCodeScanner + Issue.create) važi i za njih.
 */
class ZabranjenoDetector : Detector(), SourceCodeScanner {

    // Lint poziva visitMethodCall SAMO za pozive funkcija sa ovim imenima -
    // jeftin pre-filter pre skupljeg rada u samoj poseti.
    override fun getApplicableMethodNames(): List<String> = listOf("zabranjeno")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Poziv funkcije `zabranjeno()` nije dozvoljen (probno pravilo).",
        )
    }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            // ID se pojavljuje u izveštajima i u @Suppress anotacijama.
            id = "ZabranjenoPoziv",
            briefDescription = "Poziv probne funkcije zabranjeno()",
            explanation = """
                Probno pravilo koje dokazuje da je custom lint lanac ispravno \
                povezan. Svaki poziv funkcije `zabranjeno()` prijavljuje se \
                kao greška nivoa error i obara build.
            """,
            category = Category.CORRECTNESS,
            priority = 6,
            // ERROR namerno: hoćemo da vidimo da kapija stvarno blokira.
            severity = Severity.ERROR,
            implementation = Implementation(
                ZabranjenoDetector::class.java,
                Scope.JAVA_FILE_SCOPE, // pokriva i Kotlin i Java izvorne fajlove
            ),
        )
    }
}