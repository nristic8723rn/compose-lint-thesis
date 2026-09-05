# Faza 4a — izbor projekta za evaluaciju (istraživački izveštaj)

**Cilj:** naći JEDAN pravi open-source Android/Compose projekat koji može da nosi
trend tehničkog duga kroz istoriju (12–18 mernih tačaka). Timebox 3h. Ovo je
najrizičnija tačka rada: ako se stari komiti ne builduju, nema trenda.

**Preporuka NIJE konačna odluka** — bira se u chatu na osnovu ovog izveštaja.

## Šira lista kandidata (metapodaci sa GitHub API-ja, sept. 2026)

| Projekat | ★ | Nastao | Zadnja akt. | Veličina | Licenca | Kotlin/Java | Ocena za nas |
|---|---|---|---|---|---|---|---|
| **Automattic/pocket-casts-android** | 2832 | 2022-06 | aktivan | 98 MB | MPL-2.0 | 100% Kotlin | Prava app (podcast), zrela → migracija; proveriti ključeve |
| **spacecowboy/Feeder** | 3008 | 2014-08 | aktivan | 112 MB | GPL-3.0 | 100% Kotlin | Prava RSS app (F-Droid), XML→Compose migracija, bez ključeva |
| **duckduckgo/Android** | 4814 | 2017-01 | aktivan | 353 MB | Apache-2.0 | ~99% Kotlin | Pravi browser, migracija; OGROMAN → build rizik |
| **JunkFood02/Seal** | 28763 | 2022-04 | aktivan | 12 MB | GPL-3.0 | 100% Kotlin | Prava utility app (yt-dlp GUI), Compose-native, MALI → lako se buildu-je |
| **element-hq/element-x-android** | 2369 | 2022-10 | aktivan | 119 MB | AGPL-3.0 | 100% Kotlin | Matrix klijent, Compose-native rewrite; složen build (Matrix SDK) |
| AntennaPod/AntennaPod | 8128 | 2012-07 | aktivan | 116 MB | GPL-3.0 | **99% Java** | ISPADA: gotovo bez Kotlina/Compose |
| tuskyapp/Tusky | 2564 | 2017-01 | 2025-05 (ugašen) | 68 MB | GPL-3.0 | Kotlin, View-based | ISPADA: ugašen (fork Pachli), pretežno XML |
| chrisbanes/tivi | 6727 | 2017-08 | 2024-11 | 72 MB | Apache-2.0 | 100% Kotlin | ISPADA: showcase (kriterijum 1), neaktivan |
| CatimaLoyalty/Android | 1682 | 2019-11 | aktivan | 40 MB | GPL-3.0 | Kotlin/Java | slabo: pretežno View/XML, malo Compose |

### Zašto ispadaju showcase/neaktivni
- **AntennaPod** je star Java projekat (2012) — 99% Java, Compose zanemarljiv;
  ne nosi Compose dug (naučeno u 2g da showcase kod ne pali pravila — ovde je
  suprotan problem: premalo Compose koda uopšte).
- **Tivi** je namerno showcase (Chris Banes, „kako se pravi Compose app") — tačno
  ono što kriterijum 1 isključuje; uz to neaktivan od kraja 2024.
- **Tusky** ugašen 2025 (nastavljen kao Pachli), pretežno View-based.

## Uža lista (3 za praktičnu proveru)

1. **Feeder** — prava RSS app, 100% Kotlin, XML→Compose migracija, bez ključeva,
   srednja veličina. Najbolji „dug realno nastaje" profil.
2. **Pocket Casts** — prava, poznata podcast app, 100% Kotlin, aktivna. Rizik:
   Automattic projekti umeju da traže secrets/gradle podešavanje.
3. **Seal** — mala, Compose-native, gotovo sigurno buildabilna i za stare komite;
   „sidro" koje garantuje bar jednog upotrebljivog kandidata. Slabija strana:
   Compose od početka (nema migracije), pa je „porast duga" blaži.

## Praktična provera (build newest + build ~12mo + naša 4 pravila)

### Rezultati (naša 4 pravila; broj nalaza po pravilu: Stanje / Alokacija / Nestabilan / Hardkod)

Okruženje po lekcijama: `JAVA_HOME` → JBR 21 (za wrapper download i AGP 9.x),
`ANDROID_HOME` env (ne dira se njihov `local.properties`), `lintChecks(files(jar))`
jedna linija. Naš jar: lint-api 31.7.3.

| Projekat | Komit | Datum | AGP/Gradle | Build? | Stanje | Alok | Nestab | Hardkod |
|---|---|---|---|---|---|---|---|---|
| **Seal** | 63bd8a4d | 2026-08-25 (newest) | 8.7.2 / 8.10.2 | DA | 0 | 1 | 14 | 12 |
| **Seal** | e5a8a51f | 2025-07-25 (~12mo) | 8.7.2 / 8.10.2 | DA | 0 | 1 | 14 | 12 |
| **Feeder** | 328ffdee | 2026-08-30 (newest) | 9.x / 9.4.1 | DA | 0 | 0 | **14** | **17** |
| **Feeder** | 24022416 | 2024-08-23 (~24mo) | 8.x / 8.4 | DA | 0 | 0 | **1** | **7** |
| Pocket Casts | 78bdce4 | 2026-09-05 (newest) | — / 9.7.1 | delimično* | — | — | — | — |

\* Pocket Casts se konfiguriše (ima `app/` + `modules/`), ali referencira
`secretsFile` (za _signed release_) i koristi Gradle 9.7.1 (velik multi-modul
download); lint nije do kraja pušten u timebox-u. Nije prepreka za debug, ali
dodaje trenje — ostaje kao rezerva, nije primarni izbor.

### Ključni nalazi

1. **Feeder pokazuje PRAVI trend.** Za ~2 godine (2024-08 → 2026-08):
   `NestabilanTipParametra` 1 → 14, `HardkodovaniString` 7 → 17. Dug realno
   RASTE kako se Compose kod širi — tačno ono što radu treba (za razliku od
   uglačanog showcase-a). Oba kraja raspona se BUILDUJU (Gradle 8.4 star, 9.4.1
   nov, oba pod JBR 21). Ovo je odlučujuće: stari komiti su upotrebljivi.
2. **Seal se builduje i star i nov, ali je trend RAVAN** (14/12 na oba kraja).
   Mala, Compose-native, zrela app — malo se menja. Dobar „siguran" kandidat
   ali slaba priča o porastu duga. Napomena: identični brojevi mogu delom biti
   i Gradle up-to-date keš (lekcija 5) — u Fazi 4 forsirati clean lint.
3. **Pravilo 1 (StanjeBezRemember) = 0 na SVA ČETIRI merenja** (+ Jetsnack iz
   2g = 5 realnih projekata). Ovo JAKO potvrđuje residual iz faze 2c: doseg
   pravila 1A na bibliotečke composable lambde (bytecode) je šire ograničenje
   nego što smo mislili — na realnom kodu se praktično ne pali. Materijal za
   kritičku diskusiju (Google-ov `UnrememberedMutableState` pokriva taj procep).
4. **Feeder trošak merenja:** njegov `lint { ignoreWarnings = true }` sakriva
   naša 3 WARNING pravila; za merenje se privremeno gasi (`= false`) — to je
   orkestracija NAŠEG merenja (kao gašenje baseline-a u 3c), ne izmena njihove
   app logike. Isti obrazac se očekuje na drugim projektima.

## Preporuka (odluka pada u chatu)

**Feeder** (spacecowboy/Feeder), GPL-3.0, F-Droid RSS čitač.

- **Za:** prava app u upotrebi (ne showcase); 100% Kotlin; postepena XML→Compose
  migracija sa DOKAZANIM porastom duga kroz vreme; oba kraja 2-godišnjeg raspona
  se buildu-ju; bez ključeva/secrets; srednja veličina; permisivna licenca za
  analizu (GPL-3.0).
- **Rizik (iskreno):** (a) pravilo 1 se ne pali (kao svuda) — trend nose pravila
  2 i 3; (b) potrebno gasiti `ignoreWarnings` za merenje; (c) tačan broj mernih
  tačaka 12–18 kroz istoriju TREBA proveriti build-om na još 2–3 tačke između
  (2024 i 2026) pre pune Faze 4 — moguć AGP/Gradle drift na nekim mesecima.
- **Rezerva:** Seal (garantovano buildabilan, ali ravan trend); Pocket Casts
  (prava, veća priča, ali build trenje sa secrets/velikim multi-modulom).

**Iskren rizik izbora:** glavni nosač trenda su pravila 2 i 3; da rad ne bi
zavisio od jednog pravila, u Fazi 4 pratiti sva tri broja (ukupno/zatečeno/novo)
po pravilu i eksplicitno reći da 1A na realnom kodu ćuti (to je nalaz, ne rupa).

