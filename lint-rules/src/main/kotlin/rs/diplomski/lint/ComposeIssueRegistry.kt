package rs.diplomski.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/**
 * Centralni registar svih custom lint pravila u ovom radu.
 *
 * Lint pronalazi ovu klasu preko "Lint-Registry-v2" atributa
 * u manifestu jar fajla (videti lint-rules/build.gradle.kts).
 *
 * Svako novo pravilo (Issue) dodaje se u listu [issues].
 */
class ComposeIssueRegistry : IssueRegistry() {

    // ZabranjenoDetector.ISSUE se NAMERNO više ne isporučuje (faza 2g):
    // probno pravilo je odradilo svoju misiju (dokaz lanca u fazi 1), pa je
    // sklonjeno iz isporuke da izveštaji ne bi imali lažni šum. Klasa i njeni
    // testovi ostaju u repou kao artefakt rada. Registar isporučuje tačno 4
    // pravila (Issue-a): 2 iz pravila 1, pravilo 2 i pravilo 3.
    override val issues: List<Issue> = listOf(
        // Pravilo 1 - registrovano ODMAH, jer testovi zaobilaze
        // registar (gađaju Issue direktno) pa zaboravljenu registraciju
        // ne bi uhvatili; aplikacija bi tiho ostala bez pravila.
        RememberDetector.ISSUE_STANJE,
        RememberDetector.ISSUE_ALOKACIJA,
        // Pravilo 3 - hardkodovani stringovi u Compose UI (I18N).
        HardkodovaniStringDetector.ISSUE,
        // Pravilo 2 (v1) - nestabilan tip parametra @Composable funkcije.
        StabilnostDetector.ISSUE,
    )

    // API nivo lint-a sa kojim su pravila kompajlirana.
    override val api: Int = CURRENT_API

    // Najniži API nivo lint-a sa kojim pravila rade.
    override val minApi: Int = 8

    // Metapodaci o autoru pravila - lint ih prikazuje uz svaki nalaz,
    // da korisnik zna kome da prijavi lažno upozorenje.
    override val vendor: Vendor = Vendor(
        vendorName = "Diplomski rad - statička analiza Compose koda",
        identifier = "rs.diplomski.lint",
        feedbackUrl = "https://github.com/nristic8723rn/compose-lint-thesis/issues",
    )
}