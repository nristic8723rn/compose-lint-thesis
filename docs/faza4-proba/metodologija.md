# Faza 4b — metodologija evaluacije (Feeder kroz istoriju)

Skripta: `scripts/eval-feeder.sh`. Rezultati: `docs/faza4-proba/feeder-trend.csv`
(+ `eval-log.txt`). Trend izveštaj: `docs/faza4-proba/feeder-trend.html`.

## Šta skripta radi

Za `spacecowboy/Feeder`, za PRVI komit svakog meseca u rasponu **2024-08 →
2026-08** (cilj 18–24 tačke):

1. `git checkout` komita (author date se čuva za `--datum`);
2. u `app/build.gradle.kts` ubaci `lintChecks(files(<naš jar>))` (jedna linija);
3. pokrene lint (`:app:lintFdroidDebug` ili `:app:lintDebug`, zavisno od toga
   da li komit ima `fdroid` flavor) sa `--rerun-tasks`;
4. metrika CLI: `izvestaj --projekat feeder --datum <author date> ...` dopiše
   CSV red.

## Metodološke napomene (eksplicitno, za rad)

- **`--rerun-tasks`:** lint se tera da se izvrši nanovo na svakom komitu, da
  Gradle „up-to-date" keš (lekcija 5) ne bi preneo rezultate jednog komita na
  drugi. Bez toga bi trend mogao biti lažno ravan.
- **Neutralizacija `ignoreWarnings`:** Feeder-ov `lint { ignoreWarnings = true }`
  guši sva upozorenja, a 3 od naša 4 pravila su WARNING. Za merenje se privremeno
  postavlja `= false`. **Ovo je jedina izmena tuđe konfiguracije i služi
  ISKLJUČIVO da naši nalazi budu vidljivi** — ne menja logiku aplikacije. Vraća
  se `git checkout -- .` posle svake tačke.
- **Bez našeg baseline-a:** Feeder nema `lint-baseline.xml` za naša pravila, pa
  je `zatečeno = 0` i `novo = ukupno` na svakoj tački; `baseline_prisutan=false`.
  Trojna metrika ovde meri PORAST ukupnog duga kroz istoriju (ne efekat kapije —
  kapija se demonstrira na sample-app u fazi 3c).
- **Otpornost:** ako komit ne builduje (AGP/Gradle/JDK drift na starijim
  mesecima), razlog se upiše u `eval-log.txt` i petlja NASTAVLJA. Preskočeni
  komitovi se NE popunjavaju procenom — prazne tačke ostaju prazne.
- **Parcijalni rezultati:** CSV se dopisuje posle SVAKE tačke (ne na kraju), pa
  prekid ne gubi već izmereno.
- **Pravilo 1 (StanjeBezRemember):** očekivano 0 kroz celu istoriju (residual iz
  2c, verifikovan u `docs/faza4-verifikacija.md`). Trend nose pravila 2 i 3.

## Rezultat

Zbog čestih prekida (dugačka petlja od 25 komita nije završavala pre kraja
sesije), evaluacija je izvedena kao **kurirani skup od 6 mernih tačaka**
raspoređenih kroz 2 godine (prvi komit izabranih meseci), svaka građena
pojedinačno. Sve 6 uspešno izmereno (0 preskočeno u finalnom skupu).

**Faktografski (bez interpretacije — interpretacija ide u rad):**

| Datum | KLOC | @Composable | Stanje | Alok | Nestab | Hardkod | pond. zbir (ukupno) | dug/KLOC |
|---|---|---|---|---|---|---|---|---|
| 2024-08-05 | 34.22 | 353 | 0 | 0 | 1 | 7 | 8.40 | 0.245 |
| 2025-01-31 | 35.71 | 367 | 0 | 0 | 1 | 8 | 9.40 | 0.263 |
| 2025-04-28 | 37.89 | 379 | 0 | 0 | 10 | 17 | 31.00 | 0.818 |
| 2025-07-31 | 37.52 | 373 | 0 | 0 | 10 | 17 | 31.00 | 0.826 |
| 2026-02-02 | 38.12 | 375 | 0 | 0 | 10 | 17 | 31.00 | 0.813 |
| 2026-08-02 | 43.31 | 388 | 0 | 0 | 12 | 16 | 32.80 | 0.758 |

- Ponderisani zbir (ukupno) raste 8.40 → 32.80 kroz raspon; najveći skok između
  2025-01 i 2025-04 (9.40 → 31.00).
- `NestabilanTipParametra`: 1, 1, 10, 10, 10, 12. `HardkodovaniString`: 7, 8, 17,
  17, 17, 16. `StanjeBezRemember` i `SkupaAlokacijaBezRemember`: 0 na svim tačkama.
- `dug_po_kloc` (ukupno): 0.245 → 0.826 (do 2025-07), pa blago opada na 0.758
  (2026-08) dok KLOC raste 34→43 i @Composable 353→388.
- Feeder nema naš baseline → `novo = ukupno`, `baseline_prisutan=false` svuda.

**Build-otpornost (nalaz za rad):** 3 novija komita (Gradle 9.x) prvo su pala na
Windows file-lock nad `lint-cache/.../migrated-jars/*.jar` (zaostali Gradle
daemon drži jar); rešeno dodavanjem `--no-daemon` (svaki lint izlazi i otpušta
lock). Stariji komiti (Gradle 8.x) prošli bez toga. Takođe: checkout je vraćao
non-zero zbog locka nad `gradle-wrapper.jar` iako je HEAD ispravno pomeren —
skripta zato proverava HEAD, ne exit kod. (Dodato u lekcije.)

Artefakti: `feeder-trend.csv`, `feeder-trend.html`.

## Provera platoa i preloma (faza 4c — git-istorijska analiza)

**Plato (2025-04 → 2026-02) je STVARAN, nije artefakt merenja.** U tom rasponu
Feeder ima 219 komita, ali fajlovi koji nose najviše nalaza su nepromenjeni:
`LinearArticleContent.kt` (dominantni izvor NestabilanTip) i `TextSettings.kt`
(dominantni izvor Hardkod) su **bajt-identični** izmedju c5366d88 (2025-04) i
e24d2df1 (2026-02); od 7 fajlova-nosilaca samo 2 sporedna (OpenAISection.kt,
Settings.kt) su dirana, bez promene agregata. Uz `--rerun-tasks` (koji nezavisno
isključuje Gradle keš), identični brojevi (10/17) su logička posledica
nepromenjenog izvora — dakle stvarna stabilnost duga, ne greška merenja.

**Prelom (2025-01 → 2025-04, pond. zbir 9.4 → 31.0) mapira na konkretne feature-e.**
U tom rasponu: `LinearArticleContent.kt` +206/−125 (feat #567 „anchor links" — uveo
`idToIndex: Map<String, Int>` parametre → NestabilanTip 1→10) i `TextSettings.kt`
+704/−0 (feat #700 „custom fonts" → HardkodovaniString 8→17). Porast duga nije
difuzan nego dolazi iz prepoznatljivih komita, što je jak materijal za rad
(dug ulazi zajedno sa funkcionalnošću).

## Lokalizacija preloma (faza 4c — 3 dodatne tačke)

Dodate 3 merne tačke u prelomu (2025-02-15, 2025-03-08, 2025-04-05) razlažu
jedinstveni skok (9.4 → 31.0) na DVA feature-koraka:

| Datum | Nestab | Hardkod | pond. zbir |
|---|---|---|---|
| 2025-01-31 | 1 | 8 | 9.4 |
| 2025-02-15 | **10** | 8 | 22.0 |
| 2025-03-08 | 10 | 8 | 22.0 |
| 2025-04-05 | 10 | 8 | 22.0 |
| 2025-04-28 | 10 | **17** | 31.0 |

- Skok NestabilanTip (1→10) dešava se do 2025-02-15 — poklapa se sa feat #567
  „anchor links" (komit a6f64e35, 2025-02-07), koji je uveo `Map<String,Int>`
  parametre u `LinearArticleContent.kt`.
- Skok HardkodovaniString (8→17) dešava se izmedju 2025-04-05 i 2025-04-28 —
  poklapa se sa feat #700 „custom fonts" (komit 41fc7178, 2025-04-06), koji je
  dodao hardkodovane stringove u `TextSettings.kt`.

Ukupno 9 mernih tačaka. Prelom je time lokalizovan na dva prepoznatljiva
funkcionalna komita — dug ulazi zajedno sa funkcijama, a ne difuzno.
