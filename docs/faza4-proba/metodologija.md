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

> Broj uspelih / palih tačaka i zapažanja popunjavaju se po završetku skripte.
