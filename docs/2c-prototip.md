# Faza 2c — prototip pravila 2 (nestabilni tipovi): izveštaj spike-a

**Datum:** 2026-07-09 · **Status:** timeboxovan spike, ODGOVORI (ne pravilo).

Cilj ovog spike-a nije bilo gotovo pravilo, nego **odgovori na tri pitanja**,
svaki potkrepljen izvršnim probnim kodom (ne mišljenjem). Odluka o obimu
pravila 2 i o eventualnom dizanju potpunosti pravila 1 **NIJE** donesena ovde —
ostaje za chat, na osnovu ovog izveštaja.

## Probni kod (izvršni dokaz)

Sav dokaz je u **test** izvorima (spike, nije registrovan u
`ComposeIssueRegistry`, ne isporučuje se):

- `lint-rules/src/test/kotlin/rs/diplomski/lint/proba/StabilnostProbaDetector.kt`
  — probni detektor koji NE sudi, nego za svaki parametar `@Composable`
  funkcije ispisuje šta je uspeo da razreši (grep-friendly poruka):
  `PARAM name=… fqn=… STABLE=… IMMUTABLE=… COLLECTION=… DATA=… ALLVALPRIM=… ANNOS=[…]`
  i za svaku lambdu: `LAMBDA poziv=… param=… paramAnno=… typeAnno=…`.
- `lint-rules/src/test/kotlin/rs/diplomski/lint/proba/StabilnostProbaTest.kt`
  — tri zelena testa; svaki `expectContains(...)` je jedna **dokazana**
  činjenica. Da tvrdnja nije istinita, test bi pao.

Pokretanje: `gradlew :lint-rules:test --tests "rs.diplomski.lint.proba.*"`.

## Sažetak

| Pitanje | Signal | Isti modul (izvor) | Drugi modul (bytecode) |
|---|---|---|---|
| 1a | @Stable / @Immutable | **DA** | **DA** (mehanizam dokazan) |
| 1b | kolekcija (List/Set/Map) | **DA** | **DA** |
| 1c | data klasa, sva val primitivna/String polja | **DA** | **DELIMIČNO** |
| 2 | @Composable na tipu lambda-parametra | **DA** | **DELIMIČNO** (residual) |

---

## Pitanje 1 (nosivo) — razrešeni tipovi parametara i signali stabilnosti

**Odgovor: DA za sva tri signala (za izvorne tipove iz istog modula).**

Iz `UMethod.uastParameters` → `param.type` → `evaluator.getTypeClass(...)`
pouzdano se dolazi do razrešene `PsiClass`, i na njoj se svi signali čitaju.

Dokaz (test `pitanje1...`, stvarni izlaz detektora):

```
PARAM name=s  fqn=test.StabilanTip     STABLE=true  IMMUTABLE=false ...
PARAM name=n  fqn=test.NepromenljivTip STABLE=false IMMUTABLE=true  ...
PARAM name=lista fqn=java.util.List ... COLLECTION=true ...
PARAM name=mapa  fqn=java.util.Map  ... COLLECTION=true ...
PARAM name=dp fqn=test.DataPrim   ... DATA=true ALLVALPRIM=true
PARAM name=dn fqn=test.DataNePrim ... DATA=true ALLVALPRIM=false
PARAM name=o  fqn=test.Obican     ... DATA=false ALLVALPRIM=false
```

- **(a) @Stable/@Immutable — DA.** Čita se sa razrešenog tipa
  (`PsiClass.toUElement<UClass>().uAnnotations`). Ispravno razlikuje stabilan
  od nepromenljivog od običnog tipa.
- **(b) kolekcija — DA, uz važnu nijansu.** `List`/`Map` se na PSI nivou
  razrešavaju u **`java.util.List` / `java.util.Map`**, NE u
  `kotlin.collections.*`. Provera u pravilu MORA da gleda `java.util.*` FQN
  (ili oba skupa). Ovo je konkretan implementacioni detalj koji lako promakne.
- **(c) data klasa sa isključivo val primitivnim/String poljima — DA.**
  „data" se detektuje preko prisustva `copy()` + `component1()` metoda; „val"
  preko `PsiField.FINAL`; „primitivno/String" preko `PsiPrimitiveType` /
  `java.lang.String`. `DataPrim(val a: Int, val b: String)` → `ALLVALPRIM=true`;
  `DataNePrim(val x: StabilanTip)` → `ALLVALPRIM=false`. Heuristika radi.

## Pitanje 2 (vezano, za pravilo 1) — @Composable na tipu lambda-parametra

**Odgovor: DA za izvorne tipove; DELIMIČNO (nedokazano) za biblioteke.**

Iz `ULambdaExpression` → roditeljski `UCallExpression` →
`evaluator.computeArgumentMapping(...)` dobija se `PsiParameter` kome je lambda
prosleđena. `@Composable` je vidljiv **na TIPU parametra**
(`param.type.annotations`), ne na samom parametru.

Dokaz (test `pitanje2...`):

```
LAMBDA poziv=Column param=content paramAnno=false typeAnno=true
LAMBDA poziv=naKlik param=akcija  paramAnno=false typeAnno=false
```

Composable lambda (`Column { }`) je **razlučiva** od ne-composable (`naKlik { }`).
Ovo je tehnički preduslov za dizanje potpunosti pravila 1 (lambda kompromis):
umesto „ćuti u BILO KOJOJ lambdi", moglo bi „ćuti samo u ne-composable lambdi".

> **Residual (KLJUČNO za odluku u chatu):** dokaz je za `Column` definisan kao
> **izvor** u testu. U `sample-app`-u `Column` dolazi iz Compose **biblioteke**
> (bytecode). Da li `type-use` anotacija `@Composable` na funkcijskom tipu
> preživljava u `param.type.annotations` kad se tip čita iz bytecode-a **nije**
> nezavisno provereno u ovom timeboxu — Compose nije na classpath-u lint-rules
> modula (svesno, videti CLAUDE.md). Ovo treba proveriti protiv realnog Compose
> `Column`-a PRE nego što se potpunost pravila 1 digne.

### Residual — razrešeno (KORAK 1, mereno na sample-app)

**Ishod: NEPRIJAVLJENO → bytecode `@Composable` NE preživljava (nije čitljiv
preko `param.type.annotations`).**

Merenje: u `sample-app` `Greeting()` privremeno dodat `mutableStateOf(0)` BEZ
`remember` unutar PRAVOG `androidx.compose.foundation.layout.Column { }` bloka
(iz biblioteke → bytecode), pa `gradlew :sample-app:lintDebug`. Stvarni izlaz
(`lint-results-debug.txt`) na liniji poziva:

```
MainActivity.kt:34: Error: ... [UnrememberedMutableState from androidx.compose.runtime]   # Google, ugrađeni
MainActivity.kt:34: Information: ... [AutoboxingStateCreation from androidx.compose.runtime]
MainActivity.kt:35: Warning: Hardkodovan tekst ... [HardkodovaniString from rs.diplomski.lint]  # NAŠE (dokaz da je detektor učitan)
```

Naše `StanjeBezRemember` se **NE pojavljuje** — iako naš detektor radi
(`HardkodovaniString` puca na susednoj liniji). Znači `jeComposableLambda` za
realni bytecode `Column` vraća `false`: type-use anotacija `@Composable` na
funkcijskom tipu parametra sadržaja se ne vidi kroz `param.type.annotations`
kad tip dolazi iz `.class`-a. (Za razliku od klasnih anotacija — nalaz 3a — koje
preživljavaju; type-use anotacije na funkcijskim tipovima su drugi mehanizam.)

**Posledica (dokumentovana, ne odlučena ovde):** izmena potpunosti pravila 1
(KORAK 2) je ispravna i BEZBEDNA (bez pozitivnog dokaza → ćuti → nema lažnih
uzbuna), ali je u **produkciji INERTNA** za bibliotečke composable-e
(`Column`, `Row`, `Box`, `Scaffold`… svi bytecode). Dobitak potpunosti realno
važi samo za composable lambde dostupne kao IZVOR (isti modul). Za baš ovaj
procep, Google-ov ugrađeni `UnrememberedMutableState` ima bolji doseg — korisna
tačka za pozicioniranje u radu (naša vrednost je drugde: ERROR-severity politika,
integracija okvira, kombinovani izveštaj). Da li izmenu zadržati, tražiti bolji
način čitanja type-use anotacija iz bytecode-a, ili je vratiti — odluka za chat.

## Pitanje 3 (obim) — signali za tipove iz drugih modula (bytecode, bez izvora)

**Test `pitanje3...`** koristi `List`/`Pair`/`Observable` koji NEMAJU izvor u
testu — lint ih razrešava iz `kotlin-stdlib` / JDK bytecode-a.

Dokaz (stvarni izlaz):

```
PARAM name=lista fqn=java.util.List   ... COLLECTION=true  DATA=false ANNOS=[]
PARAM name=par   fqn=kotlin.Pair      ... COLLECTION=false DATA=true  ALLVALPRIM=false ANNOS=[kotlin.Metadata]
PARAM name=depr  fqn=java.util.Observable ... ANNOS=[java.lang.Deprecated]
```

- **(b) kolekcija — DA cross-modul.** `java.util.List` iz JDK bytecode-a se uredno
  prepoznaje (`COLLECTION=true`). Kolekcije su stdlib/JDK → uvek dostupne.
- **(c) data klasa — DELIMIČNO cross-modul.** Detekcija „data" preko
  `copy()`/`componentN()` **radi iz bytecode-a** (`kotlin.Pair` → `DATA=true`;
  ti metodi su realni JVM metodi u `.class`). ALI provera val-primitivnih polja
  pada za **generičke** tipove: `Pair<Int,String>` ima polja erasovana u
  `Object` → `ALLVALPRIM=false`. Za data klasu sa konkretnim primitivnim poljima
  iz biblioteke bi verovatno radila, ali to nije nezavisno probano (Pair je
  jedini zgodan stdlib primer i on je generički).
- **(a) @Stable/@Immutable — DA cross-modul (mehanizam dokazan).** Klasna
  anotacija se **čita iz bytecode-a**: `java.util.Observable` nosi `@Deprecated`
  i to se vidi (`ANNOS=[java.lang.Deprecated]`). `@Deprecated` je RUNTIME, a
  Compose `@Stable`/`@Immutable` su **BINARY** retention — obe kategorije su
  prisutne u `.class` i vidljive lint-u (samo `SOURCE` retention se briše).
  Napomena: konkretno `@Stable` nije probano jer Compose namerno nije na
  classpath-u; dokazan je mehanizam čitanja klasnih anotacija iz bytecode-a,
  a retention Compose anotacija je poznat (BINARY).

## Uzgredne lekcije (materijal za rad)

1. **Kotlin → JVM mapiranje tipova.** `List`/`Map`/`String` se na PSI nivou vide
   kao `java.util.List`/`java.util.Map`/`java.lang.String`. Provere zasnovane na
   „kotlin.*" FQN tiho ne rade — mora se gledati JVM ime.
2. **Test harnesa vrti više režima.** Podrazumevano lint pokreće detektor u više
   `TestMode`-ova (npr. `JVM_OVERLOADS` ubacuje sintetički parametar) i unakrsno
   proverava izlaz; za deterministički dump/asertacije koristiti
   `.testModes(TestMode.DEFAULT)`.
3. **„data" i „val" preživljavaju bytecode** (`copy`/`componentN`/finalna polja),
   ali **generička polja gube tip** (erasure) — granica statičke analize po
   potpisu.

## Šta OVAJ izveštaj NE radi

- Ne donosi odluku o obimu pravila 2.
- Ne menja pravilo 1 (lambda kompromis ostaje kakav jeste).

Obe odluke padaju u chatu, na osnovu gornjih nalaza — posebno na osnovu
**residual-a iz Pitanja 2** (bytecode composable lambda), koji je jedini
neproveren korak na putu ka dizanju potpunosti pravila 1.
