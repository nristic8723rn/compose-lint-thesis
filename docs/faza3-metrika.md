# Faza 3 — metrički sloj: dizajn i odluke

Modul `metrika` (čist Kotlin/JVM, CLI) pretvara jedan lint XML izveštaj +
izvorno stablo u jednu mernu tačku normalizovanog tehničkog duga, i iz niza
mernih tačaka pravi trend-izveštaj za menadžment. Parsiranje i računanje su
odvojeni od I/O (testabilnost je uslov, ne naknadna misao).

## Lanac

```
lint XML  ──parser──▶  nalazi (samo naša 4 ID-a) + broj tuđih (kontekst)
izvorno stablo ──normalizatori──▶  KLOC + broj @Composable
                    │
                    ▼
        agregat = Σ(broj × ponder)  ──▶  dug/KLOC, dug/@Composable
                    │
                    ▼
           CSV (dopisivanje) + JSON red  ──▶  trend HTML
```

## Normalizatori (oba se uvek računaju)

1. **KLOC** — broj nepraznih linija u `.kt` fajlovima izvornog stabla,
   isključujući `build/`, test izvore (`src/test`, `src/androidTest`) i
   generisani kod (segment `generated`), podeljeno sa 1000.
2. **BROJ_COMPOSABLE** — broj `@Composable` anotacija u istim fajlovima.
   **Dokumentovano ograničenje:** ovo je namerno jeftina aproksimacija po
   tekstu (regex), ne parsiranje AST-a. Preskačemo importe i komentare
   (linijske i blok), ali ivične slučajeve (npr. `@Composable` u string
   literalu) svesno ne pokrivamo — cena bi bila pun parser.

## Ponderi po pravilu (izvedeni iz merila, ne izmišljeni)

`ponder = UTICAJ × POUZDANOST`, gde je
UTICAJ ∈ {korektnost = 3, performanse = 2, održivost/proces = 1} i
POUZDANOST ∈ {visoka = 1.0, srednja = 0.7, niska = 0.4}.

| Pravilo (ID) | UTICAJ | POUZDANOST | Ponder | Obrazloženje |
|---|---|---|---|---|
| `StanjeBezRemember` | 3 (korektnost) | 1.0 (visoka) | **3.0** | Kvar korektnosti sa pouzdanom detekcijom (pozitivan dokaz, rezolucija paketa); jedino pravilo koje blokira merge. |
| `SkupaAlokacijaBezRemember` | 2 (performanse) | 0.7 (srednja) | **1.4** | Performansni nalaz, ali oslonjen na listu tipova (ne semantiku), pa srednja pouzdanost. |
| `NestabilanTipParametra` | 2 (performanse) | 0.7 (srednja) | **1.4** | Performansni nalaz; tačan po definiciji ali bučan na realnom kodu (vidi 2g), pa srednja pouzdanost. |
| `HardkodovaniString` | 1 (održivost) | 1.0 (visoka) | **1.0** | Procesno/lokalizaciono pitanje (mali uticaj), ali detekcija inline literala je pouzdana. |

**Konfigurabilnost je namerna:** ponderi nisu zakucani u agregat, nego se mogu
zadati kroz config/CLI (`Ponderi.izTeksta`). Time se u radu može pokazati
**osetljivost metrike na izbor pondera** — ako se zaključci menjaju sa malim
pomakom pondera, metrika je krhka; ako su stabilni, robusna.

## Izlazni zapis

Jedan red po mernoj tački: `projekat, commit_hash, datum, kloc, broj_composable`,
broj nalaza za **sva 4** pravila (uvek, i kad je 0), `ponderisani_zbir`,
`dug_po_kloc`, `dug_po_composable`, `ukupno_tudjih_upozorenja`. Format: CSV
(dopisivanje jednog fajla) + isti sadržaj kao JSON red. Bez baze. Decimale se
pišu sa `Locale.ROOT` (tačka), da srpski locale ne ubaci zarez u CSV.

**Polje `projekat` (ispravka 3b):** merna tačka pripada projektu; bez toga bi se
tačke različitih projekata mešale u jednu liniju (obmanjujuće). Trend se crta
PO PROJEKTU (vidi dole).

**Deljenje nulom → prazno, ne 0 (ispravka 3b):** kad je delilac 0 (nema `.kt`
ili nema `@Composable`), odnos je NULL — prazno polje u CSV-u, `null` u JSON-u.
Odsustvo podatka nije „nula duga"; red ostaje, tačka se u grafiku preskače.

## Trend izveštaj

Samostalan HTML (inline SVG, bez CDN-a, radi offline iz CI artefakta). **Po
projektu:** svaki projekat dobija svoju sekciju (svoji grafikoni, tabela,
legenda) — projekti se NIKAD ne spajaju u jednu liniju. Ako projekat ima < 3
merne tačke, iznad grafika stoji upozorenje „nedovoljno tačaka za trend". Prazne
vrednosti (deljenje nulom) se u grafiku preskaču (prekid linije). Po projektu:
grafikon obe normalizovane metrike, ISPOD njega obavezno razbijanje po pravilu
(agregat se nikad ne prikazuje sam), tabela, legenda. Namenjeno menadžmentu.

## Rešena pitanja (ratifikovano u chatu — odluke 10–12 u CLAUDE.md)

1. **Deljenje nulom → prazno (null), ne 0.0.** Vidi gore i `Metrika.agregiraj`.
2. **Datum je AUTHOR DATE komita**, koji poziva prosleđuje iz gita
   (`git show -s --format=%ad --date=short <hash>`); modul ostaje git-agnostičan.
   Podrazumevani današnji datum je SAMO za ručno puštanje, tako označen u `--help`.
3. **Razbijanje po projektu** (polje `projekat`) + upozorenje za < 3 tačke.

## Provera na stvarnim podacima (Zadatak 6, regenerisano u 3b)

Ceo lanac pušten nad dve merne tačke, u DVA odvojena izveštaja po projektu
(artefakti: `docs/faza3-proba/trend-jetsnack.html`, `trend-sample-app.html`;
zajednički CSV `trend.csv`):

| Projekat | Komit | Datum | KLOC | @Composable | Stanje/Alok/Nestab/Hardkod | Pond. zbir | Tuđih |
|---|---|---|---|---|---|---|---|
| jetsnack | `bc182640` | 2026-06-19 | 6.978 | 149 | 0 / 0 / 31 / 2 | 45.40 | 47 |
| sample-app | `05d93ef` | 2026-08-09 | 0.028 | 1 | 0 / 0 / 0 / 1 | 1.00 | 1 |

Jetsnack brojevi (Nestabilan 31, Hardkod 2) se **poklapaju** sa 2g izveštajem —
parser je ispravan. Tuđih 47 + naših 33 = 80 (ukupno upozorenja u 2g izveštaju).
Svaki projekat ima < 3 tačke, pa oba izveštaja nose upozorenje o trendu.
