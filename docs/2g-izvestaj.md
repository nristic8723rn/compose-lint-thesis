# Faza 2g — izveštaj o probnom puštanju na tuđ projekat

**Datum:** 2026-07-15 · **Cilj:** prvi osećaj kako se naša pravila ponašaju na
realnom, nezavisnom Compose projektu (NIJE formalna evaluacija — to je Faza 4).

## Projekat i okruženje

- **Projekat:** `android/compose-samples`, uzorak **Jetsnack**
  (app modul, pretežno Compose, srednje veličine).
- **Commit hash:** `bc182640f7903aa5ec77025b9fc3a015f40a6a13`.
- **Njihov stek (bleeding edge):** AGP **9.2.1**, Gradle **9.4.1**,
  Kotlin 2.3.21, compileSdk 37, Compose BOM 2026.06.00.
- **Naš stek:** lint-api **31.7.3** (AGP 8.7.3 + 23).

### Povezivanje (bez diranja njihovog koda)

Izgrađen naš jar (`gradlew :lint-rules:jar`) i u `Jetsnack/app/build.gradle.kts`
dodata **tačno jedna** linija u `dependencies` blok:

```kotlin
lintChecks(files("C:/.../compose-lint-thesis/lint-rules/build/libs/lint-rules.jar"))
```

Ništa drugo u njihovom projektu nije menjano. SDK lokacija predata kroz
`ANDROID_HOME` (env), ne kroz njihov `local.properties`.

### Prepreke pri povezivanju (za lekcije)

1. **Wrapper download je pao pod JDK 8 (PKIX/SSL).** Njihov `gradlew` se
   pokrenuo pod zatečenim `java` sa PATH-a (JRE 1.8.0_45), koji ne može TLS
   handshake sa `services.gradle.org` — ista klasa greške kao lekcija 1 iz
   CLAUDE.md. Rešeno postavljanjem `JAVA_HOME` na JBR 21 (Android Studio) za
   taj poziv; build daemon i dalje ide na JDK 17 (globalni
   `~/.gradle/gradle.properties`).
2. **SDK lokacija.** Njihov projekat nema `local.properties` (gitignore),
   pa je SDK predat kroz `ANDROID_HOME` env promenljivu.
3. **Timebox:** povezivanje je stalo u okvir (nije forsirano); glavni trošak
   je bio jednokratni download Gradle 9.4.1 + zavisnosti.

## Nalaz o kompatibilnosti lint API-ja (lekcija)

Naš jar je kompajliran protiv lint-api **31.7.3**, a Jetsnack-ov lint dolazi iz
AGP **9.2.1** (znatno noviji lint). **Uprkos tom raskoraku, sva naša pravila su
se izvršila i proizvela nalaze** — unazadna kompatibilnost lint-a je održala
checkove kompajlirane protiv starije verzije API-ja. U samom izveštaju nema
odbijanja našeg registra. (Napomena: eventualno konzolno upozorenje o verziji
custom check-a nije uhvaćeno jer je pozadinski proces prekinut na granici
sesije; ključni nalaz je pozitivan — stariji checkovi su radili na novijem
lint-u.)

## Nalazi po pravilu

Gledamo ISKLJUČIVO naša 4 isporučena ID-a; njihova postojeća upozorenja
(ukupno ~80 u izveštaju) ignorišemo.

| Pravilo (ID) | Broj nalaza |
|---|---|
| `StanjeBezRemember` | 0 |
| `SkupaAlokacijaBezRemember` | 0 |
| `HardkodovaniString` | 2 |
| `NestabilanTipParametra` | 31 |
| `ZabranjenoPoziv` | 0 (namerno se ne isporučuje) |

Artefakti (pun izveštaj): `docs/2g-artefakti/jetsnack-lint-results.{html,xml,txt}`.

## Ručna ocena uzorka (prvi osećaj šuma — NIJE Faza 4)

### HardkodovaniString (2/2 pregledano)

- `widget/ActionDemonstrationActivity.kt:44` — `Text("Launched from ${'$'}source")`
  → **deluje tačno**: hardkodovan tekst sa literalnim delom (šablon), nije
  lokalizovan.
- `ui/home/DestinationBar.kt:74` — `Text(text = "Delivery to 1600 Amphitheater Way")`
  → **deluje tačno**: goli hardkodovan literal u UI-ju.

Oba su na realnom `material3.Text` (iz biblioteke, bytecode) — potvrda da
pravilo 3 radi cross-modul, ne samo na stubovima. Oba **tačna**, bez lažnih
pozitiva u uzorku.

### NestabilanTipParametra (5/31 pregledano)

- `ui/components/Button.kt:58-59` — `backgroundGradient: List<Color>`,
  `disabledBackgroundGradient: List<Color>` → **deluje tačno** (List je
  nestabilan tip; gradijenti kao lista boja).
- `ui/home/Feed.kt:68-69` — `snackCollections: List<SnackCollection>`,
  `filters: List<Filter>` → **deluje tačno**.
- `ui/home/cart/Cart.kt:109` — `orderLines: List<OrderLine>` → **deluje tačno**.

Svih 5 pregledanih su **tehnički tačni**: parametri jesu `List<...>`, dakle
statički nestabilni po Compose modelu. **Ali:** 31 nalaz je mnogo, i mnogi su
liste boja iz teme (`List<Color>`) koje se u praksi ne menjaju — statički
nestabilne, efektivno konstantne. Prvi osećaj: pravilo 2 v1 je **precizno po
definiciji, ali bučno po zapremini**; kandidat za tiši default (npr. izuzeti
`List<Color>` ili tražiti da tip nije iz teme) — ali to je odluka za kasnije,
ne sada.

### StanjeBezRemember i SkupaAlokacijaBezRemember (0 nalaza)

- `StanjeBezRemember = 0`: konzistentno sa residual nalazom iz faze 2c — na
  bibliotečkim composable lambdama (bytecode) pravilo ćuti, a Jetsnack stanje
  drži korektno (uz `remember`). Nula je očekivana, ne sumnjiva.
- `SkupaAlokacijaBezRemember = 0`: naša lista skupih tipova je svesno
  konzervativna (Regex, SimpleDateFormat…); Jetsnack ih ne konstruiše u
  composable telu. Nula je očekivana.

## Zaključak (prvi osećaj)

- **Pravilo 3** je tačno i tiho (2 nalaza, oba prava) — deluje spremno.
- **Pravilo 2 v1** je tačno ali bučno (31) — nalazi nisu lažni, ali zapremina
  traži razmišljanje o tišem defaultu pre šire primene.
- **Pravila 1A/1B** su tiha na ovom projektu (0), što je u skladu sa njihovim
  dizajnom i poznatim ograničenjima.
- **Kompatibilnost lint API-ja** je održala checkove uprkos velikom raskoraku
  verzija — koristan pozitivan nalaz za rad.
