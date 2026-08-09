package rs.diplomski.metrika

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * ZADATAK 1: parser lint XML izveštaja.
 *
 * Ulaz je sadržaj lint XML-a (format `<issues><issue .../></issues>`).
 * Filtriramo SAMO naša 4 ID-a; tuđe nalaze ne modelujemo, ali brojimo koliko
 * ih ima (kontekst). Malformiran XML pada [MetrikaGreska]-om, ne tiho.
 *
 * Čist (bez I/O): prima String, vraća [ParsiranIzvestaj] — lako testabilno.
 */
object LintParser {

    fun parsiraj(xml: String): ParsiranIzvestaj {
        val dokument = try {
            val fabrika = DocumentBuilderFactory.newInstance().apply {
                // XXE zaštita: ne učitavamo spoljne entitete/DTD.
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isNamespaceAware = false
                isExpandEntityReferences = false
            }
            fabrika.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
        } catch (e: Exception) {
            throw MetrikaGreska("Neispravan lint XML izveštaj: ${e.message}", e)
        }

        val cvorovi = dokument.getElementsByTagName("issue")
        val nasi = mutableListOf<Nalaz>()
        var tudjih = 0

        for (i in 0 until cvorovi.length) {
            val issue = cvorovi.item(i) as? Element ?: continue
            val id = issue.getAttribute("id")
            if (id !in Pravila.SKUP) {
                tudjih++
                continue
            }
            val lokacija = prvaLokacija(issue)
            nasi += Nalaz(
                idPravila = id,
                ozbiljnost = issue.getAttribute("severity"),
                fajl = lokacija?.getAttribute("file").orEmpty(),
                linija = lokacija?.getAttribute("line")?.toIntOrNull() ?: 0,
                poruka = issue.getAttribute("message"),
            )
        }
        return ParsiranIzvestaj(nasi, tudjih)
    }

    /** Prvi <location> potomak issue-a (primarna lokacija nalaza). */
    private fun prvaLokacija(issue: Element): Element? {
        val lokacije = issue.getElementsByTagName("location")
        return if (lokacije.length > 0) lokacije.item(0) as? Element else null
    }
}
