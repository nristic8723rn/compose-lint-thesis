package rs.diplomski.lint

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
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement

/**
 * PRAVILO 2 (v1): parametar @Composable funkcije nestabilnog tipa.
 *
 * Nestabilan tip parametra obara preskakanje rekompozicije (skipping):
 * Compose ne može da dokaže da se vrednost nije promenila, pa rekomponuje
 * i kad ne treba. Kolekcijski interfejsi (List/Set/Map/Collection) su
 * klasičan primer - interfejs ne garantuje nepromenljivost implementacije.
 *
 * Algoritam (za svaki parametar @Composable funkcije):
 *  1. razrešiti tip parametra u PsiClass; ako ne uspe - ćutimo;
 *  2. da li nasleđuje java.util.Collection ili java.util.Map? (PAŽNJA:
 *     Kotlin List/Set/Map se na JVM-u vide kao java.util.* - nalaz 1b iz
 *     faze 2c; zato nasleđivanje po java.util imenu, ne po kotlin.collections);
 *  3. ako tip nosi @Stable ili @Immutable - stabilnost je zagarantovana
 *     (npr. kotlinx.collections.immutable) - ćutimo;
 *  4. inače - prijava (WARNING).
 *
 * VAN OBIMA v1 (dokumentovana ograničenja - videti CLAUDE.md): var-polja
 * data klasa, generici (type erasure), stabilnosne konfiguracije.
 */
class StabilnostDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<UMethod>> =
        listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitMethod(node: UMethod) {
                if (node.uAnnotations.none { it.qualifiedName == ANOTACIJA_COMPOSABLE }) {
                    return
                }
                for (param in node.uastParameters) {
                    val psiClass = context.evaluator.getTypeClass(param.type) ?: continue
                    if (!jeNestabilnaKolekcija(psiClass)) continue
                    context.report(
                        issue = ISSUE,
                        scope = param as UElement,
                        location = context.getLocation(param as UElement),
                        message = poruka(param),
                    )
                }
            }

            /** Kolekcijski interfejs (po java.util nasleđu) bez stabilnosne garancije. */
            private fun jeNestabilnaKolekcija(psiClass: PsiClass): Boolean {
                val kolekcija =
                    InheritanceUtil.isInheritor(psiClass, "java.util.Collection") ||
                        InheritanceUtil.isInheritor(psiClass, "java.util.Map")
                if (!kolekcija) return false
                // @Stable/@Immutable na tipu = eksplicitna garancija -> ćutimo.
                return !imaStabilnost(psiClass)
            }

            private fun imaStabilnost(psiClass: PsiClass): Boolean {
                val u = psiClass.toUElement() as? UClass ?: return false
                return u.uAnnotations.any {
                    it.qualifiedName == ANOTACIJA_STABLE || it.qualifiedName == ANOTACIJA_IMMUTABLE
                }
            }

            private fun poruka(param: UParameter): String =
                "Nestabilan tip parametra `${param.name}`: kolekcijski interfejs bez " +
                    "garancije stabilnosti obara preskakanje rekompozicije (skipping) - " +
                    "funkcija se rekomponuje i kad se sadržaj ne menja. Koristiti " +
                    "nepromenljivu kolekciju (npr. kotlinx.collections.immutable) ili tip " +
                    "označiti sa @Stable/@Immutable."
        }

    companion object {

        private const val ANOTACIJA_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val ANOTACIJA_STABLE = "androidx.compose.runtime.Stable"
        private const val ANOTACIJA_IMMUTABLE = "androidx.compose.runtime.Immutable"

        private val IMPLEMENTATION = Implementation(
            StabilnostDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "NestabilanTipParametra",
            briefDescription = "Nestabilan tip parametra @Composable funkcije",
            explanation = """
                Parametar @Composable funkcije čiji tip Compose ne može da \
                dokaže kao stabilan obara preskakanje rekompozicije: funkcija \
                se rekomponuje na svaku rekompoziciju roditelja, i kad se \
                vrednost parametra nije promenila.

                Kolekcijski interfejsi (`List`, `Set`, `Map`, `Collection`) su \
                tipičan uzrok - interfejs ne garantuje da implementacija ispod \
                nije promenljiva. Rešenje: nepromenljiva kolekcija (npr. \
                `kotlinx.collections.immutable`) ili označavanje tipa sa \
                `@Stable`/`@Immutable`.
            """,
            category = Category.PERFORMANCE,
            priority = 5,
            // WARNING, ne ERROR: performansni/heuristički nalaz, po politici
            // ozbiljnosti ne sme da blokira merge.
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION,
        )
    }
}
