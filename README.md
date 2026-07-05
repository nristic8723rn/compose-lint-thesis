# compose-lint-thesis

Praktični deo diplomskog rada: custom Android Lint pravila za Jetpack Compose,
integrisana u CI/CD kao kapija kvaliteta, sa metričkim slojem za praćenje
tehničkog duga.

## Struktura

- `lint-rules/` — čist Kotlin/JVM modul sa custom lint pravilima
  (ulazna tačka: `ComposeIssueRegistry`, registrovana kroz
  `Lint-Registry-v2` atribut u jar manifestu).
- `sample-app/` — minimalna Compose aplikacija, poligon za pravila,
  vezana kroz `lintChecks(project(":lint-rules"))`.

## Prvo pokretanje (Faza 1, korak 1 — verifikacija)

1. Otvoriti folder u Android Studiju (Ladybug ili noviji) i sačekati sync.
   - Ako projekat nema Gradle wrapper, Studio će ponuditi da ga doda;
     alternativno iz terminala: `gradle wrapper --gradle-version 8.9`
     (AGP 8.7.x zahteva Gradle 8.9+).
2. Proveriti da oba modula kompajliraju:
   ```
   ./gradlew :lint-rules:build :sample-app:assembleDebug
   ```
3. Pokrenuti lint (očekivano: PROLAZI, jer registar još nema pravila):
   ```
   ./gradlew :sample-app:lintDebug
   ```
   Izveštaji: `sample-app/build/reports/lint-results-debug.{html,xml,txt}`

Ako sve tri tačke prolaze — korak 1 je gotov.

## Zakovane verzije (NE menjati do kraja rada)

Sve verzije su u `gradle/libs.versions.toml`. Pravilo za lint API:
**lint verzija = AGP verzija + 23** (AGP 8.7.3 → lint 31.7.3).
Ako Studio ponudi upgrade AGP/Kotlin — odbiti. Verzije se beleže
u rad (poglavlje o implementaciji) i menjaju samo uz svestan razlog.

- AGP 8.7.3
- Kotlin 2.0.21
- lint-api 31.7.3
- Compose BOM 2024.12.01
- JDK 17

## Sledeći koraci (iz plana)

- [x] Korak 1: skelet repozitorijuma
- [ ] Korak 2: trivijalno "proba" pravilo (`zabranjeno()` detektor)
- [ ] Korak 3: prvi `LintDetectorTest`
- [ ] Korak 4: provera crvene linije u IDE
- [ ] Korak 5: GitHub Actions workflow
- [ ] Korak 6: branch protection na `main`
- [ ] Korak 7: dokumentovane verzije ✓ (vidi gore)
