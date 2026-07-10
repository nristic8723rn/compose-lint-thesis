package rs.diplomski.lint.proba

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * PROBNI (spike) detektor za fazu 2c. NIJE pravilo, NIJE registrovan.
 *
 * Jedina svrha: iz test harnesa da OSMOTRI koliko pouzdano se iz UAST-a
 * dolazi do razrešenih tipova i signala stabilnosti. Detektor ne sudi -
 * samo prijavljuje šta je uspeo da razreši, kao string poruku, pa test
 * asertuje prisustvo signala (expectContains). Ako signal ne može da se
 * dokaže zelenim testom -> odgovor u izveštaju je NE/DELIMIČNO.
 *
 * Format poruke (grep-friendly, jedna linija):
 *   PARAM name=<> fqn=<> STABLE=<b> IMMUTABLE=<b> COLLECTION=<b> DATA=<b> ALLVALPRIM=<b>
 *   LAMBDA poziv=<> param=<> paramAnno=<b> typeAnno=<b>
 */
class StabilnostProbaDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() =
        listOf(UMethod::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            // -------- Pitanje 1: parametri @Composable funkcije --------
            override fun visitMethod(node: UMethod) {
                if (node.uAnnotations.none { it.qualifiedName == COMPOSABLE }) return
                for (param in node.uastParameters) {
                    val psiClass = context.evaluator.getTypeClass(param.type)
                    val poruka = buildString {
                        append("PARAM name=").append(param.name)
                        append(" fqn=").append(psiClass?.qualifiedName ?: "<null>")
                        append(" STABLE=").append(imaAnotaciju(psiClass, STABLE))
                        append(" IMMUTABLE=").append(imaAnotaciju(psiClass, IMMUTABLE))
                        append(" COLLECTION=").append(jeKolekcija(psiClass))
                        append(" DATA=").append(jeDataKlasa(psiClass))
                        append(" ALLVALPRIM=").append(svaValPrimitivna(psiClass))
                        // Sve anotacije na tipu - dokaz čitanja iz bytecode-a
                        // za tipove iz drugih modula (pitanje 3).
                        append(" ANNOS=").append(sveAnotacije(psiClass))
                    }
                    context.report(PROBA, node, context.getLocation(param as org.jetbrains.uast.UElement), poruka)
                }
            }

            // -------- Pitanje 2: tip parametra kome je lambda prosleđena --------
            override fun visitCallExpression(node: UCallExpression) {
                val metod = node.resolve() ?: return
                val mapiranje = context.evaluator.computeArgumentMapping(node, metod)
                for ((arg, param) in mapiranje) {
                    if (arg !is ULambdaExpression) continue
                    val poruka = buildString {
                        append("LAMBDA poziv=").append(node.methodName)
                        append(" param=").append(param.name)
                        append(" paramAnno=").append(imaComposableParam(param))
                        append(" typeAnno=").append(imaComposableTip(param.type))
                    }
                    context.report(PROBA, node, context.getLocation(node), poruka)
                }
            }

            private fun imaAnotaciju(psiClass: PsiClass?, fqn: String): Boolean {
                val u = psiClass?.let { it.toUElement() as? UClass } ?: return false
                return u.uAnnotations.any { it.qualifiedName == fqn }
            }

            private fun sveAnotacije(psiClass: PsiClass?): String {
                val u = psiClass?.let { it.toUElement() as? UClass } ?: return "[]"
                return u.uAnnotations.mapNotNull { it.qualifiedName }.toString()
            }

            private fun jeKolekcija(psiClass: PsiClass?): Boolean =
                psiClass?.qualifiedName in KOLEKCIJE

            /** Kotlin data klasa: u bytecode-u i light klasi ima copy() + componentN(). */
            private fun jeDataKlasa(psiClass: PsiClass?): Boolean {
                val m = psiClass?.methods ?: return false
                return m.any { it.name == "copy" } && m.any { it.name == "component1" }
            }

            /** Sva polja final (val) i primitivnog/String tipa. */
            private fun svaValPrimitivna(psiClass: PsiClass?): Boolean {
                val polja = psiClass?.fields ?: return false
                if (polja.isEmpty()) return false
                return polja.all {
                    it.hasModifierProperty(PsiModifier.FINAL) && jePrimitivanIliString(it.type)
                }
            }

            private fun jePrimitivanIliString(type: PsiType): Boolean =
                type is PsiPrimitiveType || type.canonicalText in STRING_TIPOVI

            private fun imaComposableParam(param: PsiParameter): Boolean =
                param.annotations.any { it.qualifiedName == COMPOSABLE }

            private fun imaComposableTip(type: PsiType): Boolean =
                type.annotations.any { it.qualifiedName == COMPOSABLE }
        }

    companion object {
        private const val COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val STABLE = "androidx.compose.runtime.Stable"
        private const val IMMUTABLE = "androidx.compose.runtime.Immutable"

        private val KOLEKCIJE = setOf(
            "kotlin.collections.List", "kotlin.collections.MutableList",
            "kotlin.collections.Set", "kotlin.collections.MutableSet",
            "kotlin.collections.Map", "kotlin.collections.MutableMap",
            "java.util.List", "java.util.Set", "java.util.Map",
        )

        private val STRING_TIPOVI = setOf("java.lang.String", "kotlin.String")

        private val IMPL = Implementation(
            StabilnostProbaDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        @JvmField
        val PROBA: Issue = Issue.create(
            id = "StabilnostProba",
            briefDescription = "Probni (spike) signal stabilnosti",
            explanation = "Interni spike za fazu 2c. Nije pravilo.",
            category = Category.CORRECTNESS,
            priority = 1,
            severity = Severity.WARNING,
            implementation = IMPL,
        )
    }
}
