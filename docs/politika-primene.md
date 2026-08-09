# Politika primene pravila (kapija kvaliteta)

Ovaj dokument je sirovina za poglavlje o politici primene. Opisuje koja
pravila okvira blokiraju spajanje (merge), koja samo upozoravaju, i — što je
najvažnije — ZAŠTO. Politika nije proizvoljna: izvedena je iz jednog načela.

## Načelo: ozbiljnost prati pouzdanost detekcije

Pravilo sme da blokira merge (nivo `error`) samo ako detektuje **problem
korektnosti** i to **pouzdano** — to jest, prijavljuje tek kad ima pozitivan
dokaz (uspešnu rezoluciju tipa/paketa), a ćuti kad dokaza nema. Pravilo koje
radi sa heuristikom, konfigurabilnom listom ili stilskom konvencijom nosi veći
rizik lažnih upozorenja; takvo pravilo **ne sme da blokira merge** (nivo
`warning`), jer bi lažne uzbune brzo istrošile poverenje tima u kapiju i navele
ljude da je zaobilaze. Ukratko: cena lažnog pozitiva kod blokirajućeg pravila
je mnogo veća nego kod upozorenja, pa blokiramo samo ono u šta smo sigurni.

## Tabela

| Pravilo (ID) | Ozbiljnost | Blokira merge? | Obrazloženje | Poznata ograničenja |
|---|---|---|---|---|
| `StanjeBezRemember` (pravilo 1A) | ERROR | **DA** | Stanje kreirano pozivom `mutableStateOf` (i srodnih) van `remember` bloka gubi vrednost na svaku rekompoziciju — kvar korektnosti nevidljiv za dinamičko testiranje. Detekcija je pouzdana: prijavljuje se tek posle rezolucije poziva u paket `androidx.compose.runtime`. | Doseg kroz lambde: potpunost je dignuta pozitivnim dokazom (penjemo se kroz composable lambde), ali na REALNIM bibliotečkim composable-ima (npr. `Column` iz `.class`-a) `@Composable` na tipu lambde se ne vidi, pa je taj dobitak u produkciji inertan — pouzdano se hvata direktno u telu funkcije i u composable lambdama dostupnim kao izvor. |
| `SkupaAlokacijaBezRemember` (pravilo 1B) | WARNING | NE | Konstrukcija poznato skupog objekta (npr. `Regex`, `SimpleDateFormat`) u telu composable funkcije izvršava se na svaku rekompoziciju. Nalaz je koristan, ali se oslanja na listu tipova a ne na semantičku analizu, pa je rizik lažnih upozorenja veći — po načelu ne blokira. | „Skupo" je definisano LISTOM (`skupiTipovi` lint opcija u `lint.xml`), ne semantikom. Hvata samo konstruktore tipova sa (konfigurabilne, svesno konzervativne) liste; sve van liste se ne prijavljuje. |
| `HardkodovaniString` (pravilo 3) | WARNING | NE | Hardkodovan tekst u `Text` (i srodnim) composable pozivima je prepreka lokalizaciji i oblik tehničkog duga. Ovo je stilsko/procesno pravilo (I18N), ne korektnost, pa ne blokira merge. | Hvata samo inline string literale i šablone/konkatenacije sa literalnim delom; reference na konstante (npr. `companion const val`) se NE prijavljuju (preciznost pre potpunosti). Pozivi u `@Preview` funkcijama i ne-UI stringovi (`testTag`) su izuzeti. |
| `NestabilanTipParametra` (pravilo 2, v1) | WARNING | NE | Parametar @Composable funkcije nestabilnog tipa obara preskakanje rekompozicije (skipping), pa se funkcija rekomponuje i kad se vrednost nije promenila. Nalaz je performansni i heurističke prirode, pa ne blokira merge. | v1 hvata SAMO parametre čiji tip nasleđuje `java.util.Collection` ili `java.util.Map` bez `@Stable`/`@Immutable` na tipu. Van obima v1: `var`-polja data klasa, generici (type erasure — izgubljen tip elementa), i Compose stabilnosne konfiguracije. |

## Napomene uz tabelu

- **Zašto su ID-jevi razdvojeni po podslučaju.** Pravilo 1 ima dva odvojena
  Issue ID-ja (`StanjeBezRemember`, `SkupaAlokacijaBezRemember`) upravo zato
  što imaju različitu ozbiljnost i različitu politiku: jedan blokira, drugi ne.
  Odvojeni ID-jevi omogućavaju i nezavisno potiskivanje (`@Suppress`) i
  nezavisno podešavanje ozbiljnosti u `lint.xml`.
- **Probno pravilo se više ne isporučuje.** `ZabranjenoPoziv`
  (`ZabranjenoDetector`) je iz faze 1 služio kao dokaz da ceo lanac radi
  (registracija → IDE → pad lint zadatka → pad CI-ja). Njegova misija je
  završena i dokumentovana, pa je u fazi 2g sklonjeno iz isporuke da ne bi
  unosilo lažni šum u izveštaje; klasa i testovi ostaju u repou kao artefakt
  rada. Okvir zato isporučuje tačno četiri pravila iz tabele.
- **Pozicioniranje prema ugrađenim proverama.** Za procep koji `StanjeBezRemember`
  ne pokriva (bibliotečki composable-i iz bytecode-a), Google-ov ugrađeni
  `UnrememberedMutableState` ima bolji doseg. Vrednost našeg pravila je drugde:
  ERROR-ozbiljnost (politika kapije), integracija u okvir i kombinovani
  izveštaj. Detaljan dokaz i merenje: `docs/2c-prototip.md`.

## Uvođenje u postojeći projekat (baseline strategija)

Uvođenje statičke analize u živ projekat ne sme da bude „sve odjednom": realan
projekat već ima zatečeni tehnički dug, i kad bi kapija odmah blokirala svaki
zatečeni prekršaj, tim bi bio zatrpan i alat bi se ugasio prvog dana. Zato se
koristi **baseline**.

**Kako radi.** Pri uvođenju se generiše `lint-baseline.xml`, snimak SVIH
zatečenih nalaza. Od tog trenutka lint prijavljuje SAMO nove prekršaje (one
kojih nema u baseline-u); zatečeni su „zamrznuti" i ne obaraju build. Time novi
kod drži standard, a stari dug se ne ignoriše — samo se odlaže.

**Komande (sample-app kao primer).** U `lint { baseline = file("lint-baseline.xml") }`,
pa:

- generisanje/osvežavanje baseline-a: `gradlew :sample-app:updateLintBaseline`
- provera (samo novi prekršaji obaraju build): `gradlew :sample-app:lintDebug`

**Disciplina: baseline se SMANJUJE, nikad ne raste.** Broj stavki u
`lint-baseline.xml` je sam po sebi metrika duga — cilj je da monotono opada.
Novi prekršaj se NE dodaje u baseline (to bi bilo skrivanje duga); baseline se
osvežava tek pošto se zatečeni dug stvarno otkloni, čime se stavke uklanjaju.
Rast baseline-a između dva komita je crveni signal (dug koji se gura pod tepih).

**Ograničenje za metrički sloj (izmereno, ne izmišljeno).** Kad je baseline
aktivan, lint XML izveštaj sadrži SAMO nove nalaze — zatečeni (baseline-ovani)
nalazi u njemu NE postoje (provereno na sample-app: `HardkodovaniString` je
posle baseline-a 0 u izveštaju, iako stvarni dug postoji). Dakle jedan izveštaj
ne razlikuje „pokriveno baseline-om" od „novo" — vidi se samo „novo".

**Predloženo rešenje (bez izmišljanja podatka):**

1. **Parsirati i `lint-baseline.xml` istim parserom.** Baseline je u ISTOM
   `<issues><issue id=.../>` formatu, pa ga `LintParser` čita bez izmena:
   baseline fajl → broj zatečenih (pokrivenih) po pravilu; izveštaj → broj
   novih po pravilu; ukupno = zatečeno + novo. (Preporučeno — najjeftinije.)
2. **Alternativa: dva puštanja** lint-a, sa i bez baseline-a; „pokriveno" =
   ukupno (bez baseline-a) − novo (sa baseline-om).

Za trend duga preko istorije (Faza 4) metrika treba UKUPAN dug (bez baseline-a),
a za kapiju je relevantan samo priraštaj (novo) — pa se oba broja vode.
