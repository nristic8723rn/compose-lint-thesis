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
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getContainingUMethod

/**
 * PRAVILO 1: detekcija koda koji se izvršava na svaku
 * rekompoziciju umesto da bude zapamćen kroz remember.
 *
 * Algoritam (za svaki poziv funkcije/konstruktora u fajlu):
 *  1. KLASIFIKACIJA - da li je poziv fabrika Compose stanja
 *     (podslučaj A) ili konstruktor poznato skupog tipa (podslučaj B)?
 *  2. PENJANJE - ako se između poziva i obuhvatajuće funkcije nalazi
 *     BILO KOJA lambda, ćutimo. Ova jedna provera pokriva sve
 *     negativne obrasce iz dizajna: remember/rememberSaveable blok,
 *     ne-composable lambde (onClick), efekat-blokove. Svesni trošak:
 *     propuštamo prekršaje unutar composable lambdi (Column { ... }) -
 *     preciznost pre potpunosti; revizija posle prototipa pravila 2.
 *  3. KONTEKST - prijava samo ako je obuhvatajuća funkcija @Composable.
 */
class RememberDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<UCallExpression>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                // 1. KLASIFIKACIJA ------------------------------------
                val issue: Issue = when {
                    jeFabrikaStanja(node) -> ISSUE_STANJE
                    jeSkupaAlokacija(node) -> ISSUE_ALOKACIJA
                    else -> return
                }

                // 2. PENJANJE (pozitivan dokaz) -----------------------
                if (jeUnutarNecomposableLambde(node)) return

                // 3. KONTEKST -----------------------------------------
                val funkcija = node.getContainingUMethod() ?: return
                if (!jeComposable(funkcija)) return

                context.report(
                    issue = issue,
                    scope = node,
                    location = context.getLocation(node),
                    message = porukaZa(issue, node),
                )
            }

            /** Podslučaj A: mutableStateOf familija iz androidx.compose.runtime. */
            private fun jeFabrikaStanja(node: UCallExpression): Boolean {
                if (node.methodName !in FABRIKE_STANJA) return false
                // Provera pakleta štiti od tuđe istoimene funkcije.
                // Ako rezolucija ne uspe, NE prijavljujemo (preciznost
                // pre potpunosti - dokumentovano ograničenje).
                val metod = node.resolve() ?: return false
                val paket = context.evaluator.getPackage(metod)?.qualifiedName
                return paket == PAKET_RUNTIME
            }

            /** Podslučaj B: konstruktor tipa sa liste skupih. */
            private fun jeSkupaAlokacija(node: UCallExpression): Boolean {
                if (node.kind != UastCallKind.CONSTRUCTOR_CALL) return false
                val klasa = node.resolve()?.containingClass ?: return false
                return klasa.qualifiedName in skupiTipovi(context)
            }

            /**
             * Penjanje od poziva ka obuhvatajućoj funkciji uz POZITIVAN DOKAZ:
             * za svaku lambdu na putu razrešava se tip parametra kome je
             * prosleđena.
             *  - tip @Composable -> lambda se izvršava na rekompoziciju,
             *    penjemo dalje;
             *  - nije composable ILI rezolucija ne uspe -> ćutimo (dokaza nema).
             *
             * Time je lambda kompromis ZAMENJEN: umesto „ćuti u BILO KOJOJ
             * lambdi", prijavljujemo i unutar composable lambdi (Column { }),
             * a i dalje ćutimo u remember/onClick/efekt blokovima jer njihov
             * tip parametra NIJE @Composable (() -> T, suspend () -> Unit...).
             */
            private fun jeUnutarNecomposableLambde(node: UCallExpression): Boolean {
                var trenutni = node.uastParent
                while (trenutni != null && trenutni !is UMethod) {
                    if (trenutni is ULambdaExpression && !jeComposableLambda(trenutni)) {
                        return true
                    }
                    trenutni = trenutni.uastParent
                }
                return false
            }

            /** true SAMO ako je dokazano da je tip parametra lambde @Composable. */
            private fun jeComposableLambda(lambda: ULambdaExpression): Boolean {
                val poziv = lambda.uastParent as? UCallExpression ?: return false
                val metod = poziv.resolve() ?: return false
                val param = context.evaluator.computeArgumentMapping(poziv, metod)[lambda]
                    ?: return false
                return param.type.annotations.any { it.qualifiedName == ANOTACIJA_COMPOSABLE }
            }

            private fun jeComposable(funkcija: UMethod): Boolean =
                funkcija.uAnnotations.any { it.qualifiedName == ANOTACIJA_COMPOSABLE }

            private fun porukaZa(issue: Issue, node: UCallExpression): String =
                when (issue) {
                    ISSUE_STANJE ->
                        "Stanje kreirano pozivom `${node.methodName}` van `remember` bloka " +
                                "gubi vrednost na svaku rekompoziciju. " +
                                "Obaviti poziv u `remember { ... }` ili `rememberSaveable { ... }`."
                    else ->
                        "Objekat tipa `${node.resolve()?.containingClass?.qualifiedName}` " +
                                "konstruiše se na svaku rekompoziciju. " +
                                "Kreirati ga unutar `remember { ... }` ili van composable funkcije."
                }
        }

    companion object {

        private const val PAKET_RUNTIME = "androidx.compose.runtime"
        private const val ANOTACIJA_COMPOSABLE = "androidx.compose.runtime.Composable"

        private val FABRIKE_STANJA = setOf(
            "mutableStateOf",
            "mutableIntStateOf",
            "mutableLongStateOf",
            "mutableFloatStateOf",
            "mutableDoubleStateOf",
            "mutableStateListOf",
            "mutableStateMapOf",
            "derivedStateOf",
        )

        /** Podrazumevana, svesno konzervativna lista skupih tipova. */
        private val PODRAZUMEVANI_SKUPI_TIPOVI = setOf(
            "kotlin.text.Regex",
            "java.text.SimpleDateFormat",
            "java.text.DecimalFormat",
            "com.google.gson.Gson",
            "android.graphics.Paint",
        )

        /** Naziv lint opcije za proširenje liste (zarezom razdvojeni FQN-ovi). */
        private const val OPCIJA_SKUPI_TIPOVI = "skupiTipovi"

        /**
         * Podrazumevana lista + tipovi iz lint.xml konfiguracije:
         *   <issue id="SkupaAlokacijaBezRemember">
         *     <option name="skupiTipovi" value="com.firma.TeskiParser,..." />
         *   </issue>
         * Ovim je "skupo" eksplicitno definisano i proširivo, umesto
         * lažne ambicije da alat semantički zna šta je skupo.
         */
        private fun skupiTipovi(context: JavaContext): Set<String> {
            val izKonfiguracije = context.configuration
                .getOption(ISSUE_ALOKACIJA, OPCIJA_SKUPI_TIPOVI, null)
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            return PODRAZUMEVANI_SKUPI_TIPOVI + izKonfiguracije
        }

        private val IMPLEMENTATION = Implementation(
            RememberDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        /** Podslučaj A: stanje van remember bloka. */
        @JvmField
        val ISSUE_STANJE: Issue = Issue.create(
            id = "StanjeBezRemember",
            briefDescription = "Compose stanje kreirano van remember bloka",
            explanation = """
                Poziv `mutableStateOf` (ili srodne funkcije) direktno u telu \
                composable funkcije kreira NOVO stanje na svaku rekompoziciju - \
                prethodna vrednost se gubi. Stanje mora biti obavijeno u \
                `remember { }` ili `rememberSaveable { }`.

                Ova greška je nevidljiva za dinamičko testiranje: aplikacija \
                se kompajlira i radi, a kvar se ispoljava tek pri rekompoziciji.
            """,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION,
        )

        /** Podslučaj B: skupa alokacija u telu composable funkcije. */
        @JvmField
        val ISSUE_ALOKACIJA: Issue = Issue.create(
            id = "SkupaAlokacijaBezRemember",
            briefDescription = "Skupa alokacija u telu composable funkcije",
            explanation = """
                Konstrukcija poznato skupog objekta (npr. Regex, \
                SimpleDateFormat) u telu composable funkcije izvršava se na \
                SVAKU rekompoziciju. Objekat treba kreirati unutar \
                `remember { }` ili van composable konteksta.

                Lista tipova koji se smatraju skupim je konfigurabilna kroz \
                lint opciju "skupiTipovi" - podrazumevana lista je svesno \
                konzervativna.
            """,
            category = Category.PERFORMANCE,
            priority = 6,
            // WARNING, ne ERROR: pravilo radi sa listom tipova, ne sa
            // semantičkom analizom, pa je rizik lažnih upozorenja veći -
            // po našoj politici takvo pravilo ne sme da blokira merge.
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION,
        )
    }
}