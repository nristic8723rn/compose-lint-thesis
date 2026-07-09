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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * PRAVILO 3: detekcija hardkodovanih string literala kao teksta u Compose
 * UI pozivima (`Text` i srodni). Hardkodovan tekst je prepreka lokalizaciji
 * i oblik tehničkog duga - ispravno je proći kroz `stringResource(...)`.
 *
 * Algoritam (za svaki poziv u fajlu):
 *  1. KLASIFIKACIJA - da li je poziv `Text` iz material3/material paketa?
 *     Kao i kod pravila 1, oslanjamo se na rezoluciju pakleta, ne na golo
 *     ime, da bismo se zaštitili od tuđe istoimene funkcije. Ako rezolucija
 *     ne uspe - ćutimo (preciznost pre potpunosti).
 *  2. MAPIRANJE ARGUMENTA - nalazimo izraz koji je vezan za parametar `text`
 *     (radi i za imenovane i za pozicione argumente).
 *  3. PROVERA LITERALA - prijavljujemo samo ako `text` sadrži NEPRAZAN
 *     string literal: goli literal, string šablon sa literalnim delom
 *     ("Zdravo, ${'$'}ime") ili konkatenaciju sa literalom ("Cena: " + x).
 *     Reference na konstante (npr. companion `const val`) svesno NE hvatamo -
 *     to bi tražilo evaluaciju konstanti i podiglo rizik lažnih upozorenja;
 *     dokumentovano ograničenje, u duhu odluke "preciznost pre potpunosti".
 *  4. IZUZECI - preview funkcije (@Preview) su legitiman hardkod.
 *
 * Negativni obrasci (stringResource, prazan string, testTag i drugi ne-UI
 * stringovi) padaju prirodno: gledamo ISKLJUČIVO `text` parametar poziva
 * `Text`, pa string u `Modifier.testTag(...)` nikad i ne dolazi do provere.
 */
class HardkodovaniStringDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<UCallExpression>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                // 1. KLASIFIKACIJA ------------------------------------
                if (node.methodName != "Text") return
                val metod = node.resolve() ?: return
                val paket = context.evaluator.getPackage(metod)?.qualifiedName
                if (paket !in PAKETI_TEXT) return

                // 2. MAPIRANJE ARGUMENTA ------------------------------
                // Argument vezan za parametar `text` - i za Text("...") i za
                // Text(text = "..."). Ako Text nema `text` parametar (druga
                // preopterećenja, npr. AnnotatedString), ćutimo.
                val mapiranje = context.evaluator.computeArgumentMapping(node, metod)
                val tekstArg = mapiranje.entries
                    .firstOrNull { it.value.name == "text" }
                    ?.key
                    ?: return

                // 3. PROVERA LITERALA ---------------------------------
                if (!sadrziLiteralniTekst(tekstArg)) return

                // 4. IZUZECI ------------------------------------------
                if (jeUPreviewFunkciji(node)) return

                // MUTACIONI TEST (privremeno) - report iskljucen namerno.
                // context.report(
                //     issue = ISSUE,
                //     scope = node,
                //     location = context.getLocation(tekstArg),
                //     message = PORUKA,
                // )
            }

            /**
             * true ako izraz sadrži bar jedan neprazan string literal.
             * Rekurzija pokriva i šablone i konkatenaciju: oba su u UAST-u
             * `UPolyadicExpression` čiji su operandi delovi (literali +
             * ubačeni izrazi). Goli literal je `ULiteralExpression`.
             * Čist šablon bez teksta ("${'$'}ime") nema neprazan literalni
             * operand -> ne prijavljuje se (nije hardkodovan tekst).
             */
            private fun sadrziLiteralniTekst(izraz: UExpression?): Boolean {
                val e = izraz?.skipParenthesizedExprDown() ?: return false
                return when (e) {
                    is ULiteralExpression -> {
                        val v = e.value
                        v is String && v.isNotBlank()
                    }
                    is UPolyadicExpression -> e.operands.any { sadrziLiteralniTekst(it) }
                    else -> false
                }
            }

            private fun jeUPreviewFunkciji(node: UCallExpression): Boolean {
                val funkcija: UMethod = node.getContainingUMethod() ?: return false
                return funkcija.uAnnotations.any { it.qualifiedName == ANOTACIJA_PREVIEW }
            }
        }

    companion object {

        /** material3 je primaran; material2 uključen jer je provera besplatna. */
        private val PAKETI_TEXT = setOf(
            "androidx.compose.material3",
            "androidx.compose.material",
        )

        private const val ANOTACIJA_PREVIEW =
            "androidx.compose.ui.tooling.preview.Preview"

        private const val PORUKA =
            "Hardkodovan tekst u Compose UI onemogućava lokalizaciju. " +
                "Izdvojiti string u resurs i koristiti `stringResource(R.string...)`."

        private val IMPLEMENTATION = Implementation(
            HardkodovaniStringDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "HardkodovaniString",
            briefDescription = "Hardkodovan string u Compose UI tekstu",
            explanation = """
                String literal prosleđen direktno kao tekst `Text` (ili srodne) \
                composable funkcije zaključava tekst u kod - onemogućava \
                prevode i lokalizaciju, i predstavlja tehnički dug.

                Ispravno je tekst izdvojiti u string resurs i pročitati ga \
                kroz `stringResource(R.string.kljuc)`.

                Pravilo namerno hvata SAMO inline literale i šablone sa \
                literalnim delovima; reference na konstante se ne prijavljuju \
                (dokumentovano ograničenje - preciznost pre potpunosti).
            """,
            // Ugrađena kategorija za internacionalizaciju - tačno naš domen.
            category = Category.I18N,
            priority = 5,
            // WARNING, ne ERROR: stilsko/procesno pravilo, ne korektnost -
            // po politici ozbiljnosti ne sme da blokira merge.
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION,
        )
    }
}
