package rs.diplomski.metrika

import java.util.Locale

/**
 * Trend-izveštaj (faza 3c — trojna metrika). Samostalan HTML, bez CDN-a, offline.
 *
 * PO PROJEKTU (nikad spojeno), uz upozorenje za < 3 tačke. Primarni grafikon:
 * UKUPAN i NOVI dug kao dve serije — centralna slika rada (ukupan treba da
 * opada dok se dug otplaćuje, novi da stoji na nuli dok kapija radi). Zatečeni
 * se vidi u tabeli i u razbijanju po pravilu. Prazne vrednosti (deljenje nulom)
 * se u grafiku preskaču (prekid linije).
 */
object TrendIzvestaj {

    private const val MIN_TACAKA_ZA_TREND = 3

    private data class Serija(val ime: String, val boja: String, val vrednosti: List<Double?>)

    private val OPIS_PRAVILA: Map<String, String> = mapOf(
        Pravila.STANJE to "Stanje van remember bloka — ERROR, korektnost",
        Pravila.ALOKACIJA to "Skupa alokacija bez remember — WARNING, performanse",
        Pravila.NESTABILAN to "Nestabilan tip parametra — WARNING, performanse",
        Pravila.HARDKOD to "Hardkodovan string u UI — WARNING, lokalizacija/proces",
    )

    private val BOJE_PRAVILA = mapOf(
        Pravila.STANJE to "#c0392b", Pravila.ALOKACIJA to "#e67e22",
        Pravila.NESTABILAN to "#2980b9", Pravila.HARDKOD to "#27ae60",
    )

    fun generisi(tacke: List<MernaTacka>, datumGenerisanja: String): String {
        val poProjektu = tacke.groupBy { it.projekat }
        return buildString {
            append("<!doctype html>\n<html lang=\"sr\">\n<head>\n<meta charset=\"utf-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<title>Trend tehničkog duga — Compose lint okvir</title>\n")
            append("<style>\n").append(STIL).append("\n</style>\n</head>\n<body>\n")
            append("<h1>Trend tehničkog duga — Compose lint okvir</h1>\n")
            append("<p class=\"meta\">Generisano: ").append(esc(datumGenerisanja))
            append(" · Projekata: ").append(poProjektu.size).append("</p>\n")
            append("<p class=\"opis\">Trojna metrika po pravilu: <b>ukupan</b> dug (sav zatečeni + novi), ")
            append("<b>zatečeni</b> (prihvaćen u baseline pri uvođenju) i <b>novi</b> (ukupan − zatečeni). ")
            append("Poenta: ukupan dug treba da <b>opada</b> kako se otplaćuje, a novi da <b>stoji na nuli</b> ")
            append("dok kapija radi — efekat kapije je nevidljiv ako se gleda samo ukupan dug.</p>\n")
            for ((projekat, tackeP) in poProjektu) append(sekcijaProjekta(projekat, tackeP))
            append("<section class=\"legenda\">\n<h2>Šta koje pravilo znači</h2>\n<ul>\n")
            for (id in Pravila.REDOSLED) {
                append("<li><span class=\"kvadrat\" style=\"background:").append(BOJE_PRAVILA.getValue(id))
                append("\"></span><b>").append(esc(id)).append("</b> — ").append(esc(OPIS_PRAVILA.getValue(id))).append("</li>\n")
            }
            append("</ul>\n</section>\n</body>\n</html>\n")
        }
    }

    private fun sekcijaProjekta(projekat: String, tacke: List<MernaTacka>): String = buildString {
        val x = tacke.map { it.datum }
        append("<section class=\"projekat\">\n<h2>Projekat: ").append(esc(projekat))
        append(" <span class=\"broj\">(").append(tacke.size).append(" mernih tačaka)</span></h2>\n")
        if (!tacke.first().baselinePrisutan) {
            append("<p class=\"info\">Baseline nije uveden — ceo dug je „nov” (zatečeno = 0). ")
            append("Uredan primer projekta bez uvedenog okvira.</p>\n")
        }
        if (tacke.any { it.baselineZastario() }) {
            append("<p class=\"upozorenje\">⚠ Baseline je zastareo (sadrži stavke kojih više nema): ")
            append("dug je otplaćen a baseline nije osvežen. Osvežiti komandom updateLintBaseline.</p>\n")
        }
        if (tacke.size < MIN_TACAKA_ZA_TREND) {
            append("<p class=\"upozorenje\">⚠ Nedovoljno tačaka za trend (< ").append(MIN_TACAKA_ZA_TREND)
            append("). Prikazane vrednosti su tačke, ne pouzdan trend.</p>\n")
        }
        append("<h3>Ukupan i novi dug (normalizovano po KLOC)</h3>\n")
        append("<p class=\"opis\">Centralna slika: ukupan dug (opada dok se otplaćuje) i novi dug ")
        append("(stoji na nuli dok kapija ne pušta nove prekršaje).</p>\n")
        append(svgGraf(x, listOf(
            Serija("Ukupan dug / KLOC", "#8e44ad", tacke.map { it.dugPoKloc?.ukupno }),
            Serija("Novi dug / KLOC", "#c0392b", tacke.map { it.dugPoKloc?.novo }),
        )))
        append("<h3>Razbijanje po pravilu (ukupan broj nalaza)</h3>\n")
        append(svgGraf(x, Pravila.REDOSLED.map { id ->
            Serija(id, BOJE_PRAVILA.getValue(id), tacke.map { it.poPravilu.getValue(id).ukupno.toDouble() })
        }))
        append("<h3>Sirovi brojevi (po pravilu: ukupno / zatečeno / novo)</h3>\n")
        append(tabela(tacke))
        append("</section>\n")
    }

    private fun svgGraf(xOznake: List<String>, serije: List<Serija>): String {
        val w = 760; val h = 320; val levo = 56; val desno = 20; val gore = 20; val dole = 44
        val pw = w - levo - desno; val ph = h - gore - dole; val n = xOznake.size
        val maxY = maxOf(1.0, serije.flatMap { it.vrednosti }.filterNotNull().maxOrNull() ?: 1.0)
        fun x(i: Int) = if (n <= 1) levo + pw / 2.0 else levo + pw * i.toDouble() / (n - 1)
        fun y(v: Double) = gore + ph * (1.0 - v / maxY)
        val sb = StringBuilder("<svg viewBox=\"0 0 $w $h\" role=\"img\" class=\"graf\">\n")
        for (k in 0..2) {
            val v = maxY * k / 2.0; val yy = y(v)
            sb.append("<line x1=\"$levo\" y1=\"$yy\" x2=\"${levo + pw}\" y2=\"$yy\" class=\"mreza\"/>\n")
            sb.append("<text x=\"${levo - 6}\" y=\"${yy + 4}\" class=\"osa\" text-anchor=\"end\">").append(fmt2(v)).append("</text>\n")
        }
        for (i in 0 until n) {
            if (!(n <= 6 || i == 0 || i == n - 1)) continue
            sb.append("<text x=\"${x(i)}\" y=\"${h - dole + 18}\" class=\"osa\" text-anchor=\"middle\">").append(esc(xOznake[i])).append("</text>\n")
        }
        for (s in serije) {
            var i = 0
            while (i < s.vrednosti.size) {
                if (s.vrednosti[i] == null) { i++; continue }
                var j = i; val tacke = StringBuilder()
                while (j < s.vrednosti.size && s.vrednosti[j] != null) { tacke.append("${x(j)},${y(s.vrednosti[j]!!)} "); j++ }
                if (j - i >= 2) sb.append("<polyline points=\"${tacke.toString().trim()}\" fill=\"none\" stroke=\"${s.boja}\" stroke-width=\"2\"/>\n")
                for (k in i until j) sb.append("<circle cx=\"${x(k)}\" cy=\"${y(s.vrednosti[k]!!)}\" r=\"3\" fill=\"${s.boja}\"/>\n")
                i = j
            }
        }
        sb.append("</svg>\n<div class=\"legenda-serije\">")
        for (s in serije) sb.append("<span><span class=\"kvadrat\" style=\"background:${s.boja}\"></span>").append(esc(s.ime)).append("</span>")
        sb.append("</div>\n")
        return sb.toString()
    }

    private fun tabela(tacke: List<MernaTacka>): String = buildString {
        append("<table>\n<thead><tr><th>Datum</th><th>Komit</th><th>KLOC</th><th>@Comp.</th><th>Baseline</th>")
        for (id in Pravila.REDOSLED) append("<th>").append(esc(id)).append("</th>")
        append("<th>Pond. zbir</th><th>Dug/KLOC</th><th>Dug/@Comp.</th><th>Tuđih</th></tr></thead>\n<tbody>\n")
        for (t in tacke) {
            append("<tr><td>").append(esc(t.datum)).append("</td><td>").append(esc(t.commitHash.take(8))).append("</td>")
            append("<td>").append(fmt2(t.kloc)).append("</td><td>").append(t.brojComposable).append("</td>")
            append("<td>").append(if (t.baselinePrisutan) "da" else "ne").append("</td>")
            for (id in Pravila.REDOSLED) { val tb = t.poPravilu.getValue(id); append("<td>").append("${tb.ukupno} / ${tb.zateceno} / ${tb.novo}").append("</td>") }
            append("<td>").append(triCelija(t.ponderisaniZbir)).append("</td>")
            append("<td>").append(triCelijaOpt(t.dugPoKloc)).append("</td>")
            append("<td>").append(triCelijaOpt(t.dugPoComposable)).append("</td>")
            append("<td>").append(t.ukupnoTudjih).append("</td></tr>\n")
        }
        append("</tbody>\n</table>\n<p class=\"opis\">Ćelije pravila i agregata: <b>ukupno / zatečeno / novi</b>.</p>\n")
    }

    private fun fmt2(x: Double) = String.format(Locale.ROOT, "%.2f", x)
    private fun triCelija(t: TriDouble) = "${fmt2(t.ukupno)} / ${fmt2(t.zateceno)} / ${fmt2(t.novo)}"
    private fun triCelijaOpt(t: TriDouble?) = if (t == null) "—" else triCelija(t)
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private val STIL = """
        body { font-family: system-ui, Arial, sans-serif; margin: 24px; color: #1a1a1a; background: #fff; }
        h1 { font-size: 22px; } h2 { font-size: 18px; margin-top: 30px; border-top: 2px solid #eee; padding-top: 14px; }
        h3 { font-size: 15px; margin: 18px 0 6px; color: #333; }
        .meta { color: #555; } .opis { color: #444; max-width: 780px; margin: 4px 0; }
        .broj { color: #888; font-weight: normal; font-size: 14px; }
        .upozorenje { background: #fff4e5; border: 1px solid #ffcc80; color: #8a5a00; padding: 8px 12px; border-radius: 6px; max-width: 780px; }
        .info { background: #eef5fb; border: 1px solid #b6d4ea; color: #1a5276; padding: 8px 12px; border-radius: 6px; max-width: 780px; }
        .graf { width: 100%; max-width: 760px; height: auto; background: #fafafa; border: 1px solid #eee; }
        .mreza { stroke: #ddd; stroke-width: 1; } .osa { fill: #666; font-size: 11px; }
        .legenda-serije { display: flex; gap: 18px; flex-wrap: wrap; margin: 8px 0 4px; font-size: 13px; }
        .kvadrat { display: inline-block; width: 11px; height: 11px; border-radius: 2px; margin-right: 6px; vertical-align: middle; }
        table { border-collapse: collapse; font-size: 12px; }
        th, td { border: 1px solid #ddd; padding: 4px 8px; text-align: right; }
        th:first-child, td:first-child, th:nth-child(2), td:nth-child(2) { text-align: left; }
        .legenda ul { list-style: none; padding: 0; max-width: 780px; } .legenda li { margin: 6px 0; }
    """.trimIndent()
}
