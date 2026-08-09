package rs.diplomski.metrika

import java.util.Locale

/**
 * ZADATAK 5 (+ ispravka faze 3b): trend-izveštaj (samostalan HTML, BEZ CDN-a).
 *
 * PO PROJEKTU: merne tačke se grupišu po `projekat` i svaki projekat dobija
 * SVOJU sekciju (svoji grafikoni, tabela, legenda) — projekti se NIKAD ne
 * spajaju u jednu liniju. Ako projekat ima < 3 merne tačke, iznad grafika ide
 * upozorenje „nedovoljno tačaka za trend".
 *
 * Sadrži po projektu: (1) linijski grafikon obe normalizovane metrike,
 * (2) OBAVEZNO razbijanje po pravilu (4 linije), (3) tabelu sirovih brojeva.
 * Prazne vrednosti (deljenje nulom) se u grafiku PRESKAČU (prekid linije).
 */
object TrendIzvestaj {

    private const val MIN_TACAKA_ZA_TREND = 3

    private data class Serija(val ime: String, val boja: String, val vrednosti: List<Double?>)

    private val OPIS_PRAVILA: Map<String, String> = mapOf(
        Pravila.STANJE to "Stanje van remember bloka — ERROR, korektnost (gubi se na rekompoziciju)",
        Pravila.ALOKACIJA to "Skupa alokacija bez remember — WARNING, performanse",
        Pravila.NESTABILAN to "Nestabilan tip parametra — WARNING, performanse (obara skipping)",
        Pravila.HARDKOD to "Hardkodovan string u UI — WARNING, lokalizacija/proces",
    )

    private val BOJE_PRAVILA = mapOf(
        Pravila.STANJE to "#c0392b",
        Pravila.ALOKACIJA to "#e67e22",
        Pravila.NESTABILAN to "#2980b9",
        Pravila.HARDKOD to "#27ae60",
    )

    fun generisi(tacke: List<MernaTacka>, datumGenerisanja: String): String {
        val poProjektu = tacke.groupBy { it.projekat } // LinkedHashMap: čuva redosled

        return buildString {
            append("<!doctype html>\n<html lang=\"sr\">\n<head>\n")
            append("<meta charset=\"utf-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<title>Trend tehničkog duga — Compose lint okvir</title>\n")
            append("<style>\n").append(STIL).append("\n</style>\n</head>\n<body>\n")

            append("<h1>Trend tehničkog duga — Compose lint okvir</h1>\n")
            append("<p class=\"meta\">Generisano: ").append(esc(datumGenerisanja))
            append(" · Projekata: ").append(poProjektu.size).append("</p>\n")

            for ((projekat, tackeP) in poProjektu) {
                append(sekcijaProjekta(projekat, tackeP))
            }

            append("<section class=\"legenda\">\n<h2>Šta koje pravilo znači</h2>\n<ul>\n")
            for (id in Pravila.REDOSLED) {
                append("<li><span class=\"kvadrat\" style=\"background:")
                append(BOJE_PRAVILA.getValue(id)).append("\"></span><b>").append(esc(id))
                append("</b> — ").append(esc(OPIS_PRAVILA.getValue(id))).append("</li>\n")
            }
            append("</ul>\n</section>\n")

            append("</body>\n</html>\n")
        }
    }

    private fun sekcijaProjekta(projekat: String, tacke: List<MernaTacka>): String = buildString {
        val xOznake = tacke.map { it.datum }
        append("<section class=\"projekat\">\n")
        append("<h2>Projekat: ").append(esc(projekat))
        append(" <span class=\"broj\">(").append(tacke.size).append(" mernih tačaka)</span></h2>\n")

        if (tacke.size < MIN_TACAKA_ZA_TREND) {
            append("<p class=\"upozorenje\">⚠ Nedovoljno tačaka za trend (< ")
            append(MIN_TACAKA_ZA_TREND).append("). Prikazane vrednosti su tačke, ")
            append("ne pouzdan trend — potrebno je više mernih tačaka kroz vreme.</p>\n")
        }

        append("<h3>Normalizovani tehnički dug</h3>\n")
        append(svgGraf(xOznake, listOf(
            Serija("Dug po KLOC", "#8e44ad", tacke.map { it.dugPoKloc }),
            Serija("Dug po @Composable", "#16a085", tacke.map { it.dugPoComposable }),
        )))

        append("<h3>Razbijanje po pravilu (broj nalaza)</h3>\n")
        append("<p class=\"opis\">Agregat se nikad ne gleda sam — vidi se koje pravilo nosi dug.</p>\n")
        append(svgGraf(xOznake, Pravila.REDOSLED.map { id ->
            Serija(id, BOJE_PRAVILA.getValue(id), tacke.map { (it.poPravilu[id] ?: 0).toDouble() })
        }))

        append("<h3>Sirovi brojevi</h3>\n")
        append(tabela(tacke))
        append("</section>\n")
    }

    /** Inline SVG linijski grafikon; null vrednosti prave prekid linije. */
    private fun svgGraf(xOznake: List<String>, serije: List<Serija>): String {
        val w = 760; val h = 320
        val levo = 56; val desno = 20; val gore = 20; val dole = 44
        val pw = w - levo - desno; val ph = h - gore - dole
        val n = xOznake.size
        val maxY = maxOf(1.0, serije.flatMap { it.vrednosti }.filterNotNull().maxOrNull() ?: 1.0)

        fun x(i: Int): Double = if (n <= 1) levo + pw / 2.0 else levo + pw * i.toDouble() / (n - 1)
        fun y(v: Double): Double = gore + ph * (1.0 - v / maxY)

        val sb = StringBuilder()
        sb.append("<svg viewBox=\"0 0 $w $h\" role=\"img\" class=\"graf\">\n")
        for (k in 0..2) {
            val v = maxY * k / 2.0
            val yy = y(v)
            sb.append("<line x1=\"$levo\" y1=\"$yy\" x2=\"${levo + pw}\" y2=\"$yy\" class=\"mreza\"/>\n")
            sb.append("<text x=\"${levo - 6}\" y=\"${yy + 4}\" class=\"osa\" text-anchor=\"end\">")
                .append(fmt2(v)).append("</text>\n")
        }
        for (i in 0 until n) {
            if (!(n <= 6 || i == 0 || i == n - 1)) continue
            sb.append("<text x=\"${x(i)}\" y=\"${h - dole + 18}\" class=\"osa\" text-anchor=\"middle\">")
                .append(esc(xOznake[i])).append("</text>\n")
        }
        for (s in serije) {
            // Segmenti: neprekinuti nizovi ne-null tačaka -> polyline; null = prekid.
            var i = 0
            while (i < s.vrednosti.size) {
                if (s.vrednosti[i] == null) { i++; continue }
                var j = i
                val tacke = StringBuilder()
                while (j < s.vrednosti.size && s.vrednosti[j] != null) {
                    tacke.append("${x(j)},${y(s.vrednosti[j]!!)} ")
                    j++
                }
                val brojUSegmentu = j - i
                if (brojUSegmentu >= 2) {
                    sb.append("<polyline points=\"${tacke.toString().trim()}\" fill=\"none\" stroke=\"${s.boja}\" stroke-width=\"2\"/>\n")
                }
                for (k in i until j) {
                    sb.append("<circle cx=\"${x(k)}\" cy=\"${y(s.vrednosti[k]!!)}\" r=\"3\" fill=\"${s.boja}\"/>\n")
                }
                i = j
            }
        }
        sb.append("</svg>\n")
        sb.append("<div class=\"legenda-serije\">")
        for (s in serije) {
            sb.append("<span><span class=\"kvadrat\" style=\"background:${s.boja}\"></span>")
                .append(esc(s.ime)).append("</span>")
        }
        sb.append("</div>\n")
        return sb.toString()
    }

    private fun tabela(tacke: List<MernaTacka>): String = buildString {
        append("<table>\n<thead><tr>")
        append("<th>Datum</th><th>Komit</th><th>KLOC</th><th>@Composable</th>")
        for (id in Pravila.REDOSLED) append("<th>").append(esc(id)).append("</th>")
        append("<th>Pond. zbir</th><th>Dug/KLOC</th><th>Dug/@Comp.</th><th>Tuđih</th>")
        append("</tr></thead>\n<tbody>\n")
        for (t in tacke) {
            append("<tr><td>").append(esc(t.datum)).append("</td>")
            append("<td>").append(esc(t.commitHash.take(8))).append("</td>")
            append("<td>").append(fmt2(t.kloc)).append("</td>")
            append("<td>").append(t.brojComposable).append("</td>")
            for (id in Pravila.REDOSLED) append("<td>").append(t.poPravilu[id] ?: 0).append("</td>")
            append("<td>").append(fmt2(t.ponderisaniZbir)).append("</td>")
            append("<td>").append(celijaOpt(t.dugPoKloc)).append("</td>")
            append("<td>").append(celijaOpt(t.dugPoComposable)).append("</td>")
            append("<td>").append(t.ukupnoTudjih).append("</td></tr>\n")
        }
        append("</tbody>\n</table>\n")
    }

    private fun fmt2(x: Double): String = String.format(Locale.ROOT, "%.2f", x)
    private fun celijaOpt(x: Double?): String = if (x == null) "—" else fmt2(x)

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private val STIL = """
        body { font-family: system-ui, Arial, sans-serif; margin: 24px; color: #1a1a1a; background: #fff; }
        h1 { font-size: 22px; } h2 { font-size: 18px; margin-top: 30px; border-top: 2px solid #eee; padding-top: 14px; }
        h3 { font-size: 15px; margin: 18px 0 6px; color: #333; }
        .meta { color: #555; } .opis { color: #444; max-width: 720px; margin: 4px 0; }
        .broj { color: #888; font-weight: normal; font-size: 14px; }
        .upozorenje { background: #fff4e5; border: 1px solid #ffcc80; color: #8a5a00; padding: 8px 12px; border-radius: 6px; max-width: 720px; }
        .graf { width: 100%; max-width: 760px; height: auto; background: #fafafa; border: 1px solid #eee; }
        .mreza { stroke: #ddd; stroke-width: 1; } .osa { fill: #666; font-size: 11px; }
        .legenda-serije { display: flex; gap: 18px; flex-wrap: wrap; margin: 8px 0 4px; font-size: 13px; }
        .kvadrat { display: inline-block; width: 11px; height: 11px; border-radius: 2px; margin-right: 6px; vertical-align: middle; }
        table { border-collapse: collapse; font-size: 13px; }
        th, td { border: 1px solid #ddd; padding: 4px 8px; text-align: right; }
        th:first-child, td:first-child, th:nth-child(2), td:nth-child(2) { text-align: left; }
        .legenda ul { list-style: none; padding: 0; max-width: 760px; }
        .legenda li { margin: 6px 0; }
    """.trimIndent()
}
