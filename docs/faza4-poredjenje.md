# Faza 4d — poređenje sa postojećim skupovima pravila

**Merna tačka:** Feeder @ `ae132fbf` (2026-08-02). Jedno lint puštanje sa TRI
skupa aktivnih pravila istovremeno: (a) naša 4, (b) Slack compose-lints 1.4.2
(`com.slack.lint.compose:compose-lint-checks:1.4.2`, dodato preko
`lintChecks(...)`), (c) ugrađene AGP/Compose provere (već aktivne).
Podešavanje: `ignoreWarnings=false`, `--no-daemon`, `--rerun-tasks`.

Slack se ODMAH povezao i dao nalaze (potvrda: `ComposeModifierMissing from
com.slack.lint.compose:compose-lints` u izlazu). **Ovo je merenje preklapanja,
bez zaključaka ko je „bolji" — tumačenje ide u rad.**

## 1. NestabilanTipParametra vs ComposeUnstableCollections

| | broj nalaza |
|---|---|
| `NestabilanTipParametra` (naše) | 12 |
| `ComposeUnstableCollections` (Slack) | 12 |
| **IDENTIČNO (isti fajl:linija)** | **12** |
| samo naše | 0 |
| samo Slack | 0 |

**Potpuno preklapanje: svih 12 nalaza je na istom fajlu:liniji.** Nema nijedne
razlike za primer — to je sam nalaz.

Zajednički primer — `widget/FeedWidget.kt:266`, parametar `items: List<FeedWidgetItem>`:
- naše (Warning): „Nestabilan tip parametra `items`: kolekcijski interfejs bez
  garancije stabilnosti obara preskakanje rekompozicije (skipping)…"
- Slack (Warning): „The Compose Compiler cannot infer the stability of a
  parameter if a `List<FeedWidgetItem>` is used in it… You should use Kotlinx
  immutable collections…"

Ista ozbiljnost (Warning), isti obrazac (kolekcijski parametar), ista predložena
ispravka (nepromenljiva kolekcija). Svih 12 zajedničkih lokacija:

```
widget/FeedWidget.kt:266
ui/compose/feedarticle/ReaderView.kt:98
ui/compose/material3/SwipeableState.kt:580
ui/compose/html/LinearArticleContent.kt: 194, 421, 445, 481, 619, 644, 767, 819, 924
```

Napomena o obimu: naše pravilo (odluka 8) hvata parametre koji NASLEĐUJU
`java.util.Collection`/`Map` bez `@Stable/@Immutable`. Na ovom projektu daje
identičan skup kao Slack; eventualne razlike u obimu (npr. `Map` vs `List`,
`@Immutable` gate) na ovom kodu se ne ispoljavaju jer nema takvih graničnih
slučajeva.

## 2. StanjeBezRemember vs ComposeRememberMissing vs UnrememberedMutableState

| Pravilo | Izvor | broj nalaza |
|---|---|---|
| `StanjeBezRemember` | naše | 0 |
| `ComposeRememberMissing` | Slack | 0 |
| `UnrememberedMutableState` | ugrađeno (androidx) | 0 |

**Sva tri se slažu na NULI** na ovom komitu — Feeder korektno koristi `remember`
za stanje. Nema nalaza za poređenje po lokaciji (saglasnost na nuli).

Srodno, ali RAZLIČIT obrazac: `AutoboxingStateValueProperty` (ugrađeno) = 11 —
tiče se čitanja `.value` autoboxing stanja, ne izostanka `remember`-a; nije
ekvivalent nijednom od tri gornja pravila. (`AutoboxingStateCreation` = 0.)

## 3. HardkodovaniString i SkupaAlokacijaBezRemember — pokrivenost

| Pravilo | Izvor | broj | Ekvivalent u Slack/ugrađenom? |
|---|---|---|---|
| `HardkodovaniString` | naše | 16 | **NE postoji** |
| `SkupaAlokacijaBezRemember` | naše | 0 (na ovom kodu) | **NE postoji** |

**HardkodovaniString (16 nalaza) nema ekvivalent** ni u Slack compose-lints ni
u ugrađenim proverama za Compose:
- Slack compose-lints nema pravilo o string literalima u `Text` (pregledani
  Slack Compose ID-jevi dole).
- Ugrađene i18n provere gađaju druge mete: `HardcodedText` (samo XML atribut
  `android:text`), `SetTextI18n` (View `setText`) — nijedno se ne pali na
  Compose `Text("literal")` (0 takvih u izveštaju na Compose pozivima).

`SkupaAlokacijaBezRemember` je 0 na Feeder-u (nema `Regex`/`SimpleDateFormat` u
composable telu), pa nema šta da se uporedi po lokaciji; ni Slack ni ugrađeno
nemaju pravilo za skupu alokaciju u composable-u.

## Spisak pregledanih tuđih ID-jeva (prisutni u izveštaju)

**Slack compose-lints (com.slack.lint.compose):** `ComposeUnstableCollections`,
`ComposeUnstableReceiver`, `ComposableNaming`, `ComposeCompositionLocalUsage`,
`ComposeParameterOrder`, `ComposeModifierMissing`, `ComposePreviewNaming`.
`ComposeRememberMissing` je iz istog skupa (0 nalaza pa se ne vidi u zbiru).

**Ugrađene (androidx/AGP) relevantne za naša pravila:**
`UnrememberedMutableState` (0), `AutoboxingStateCreation` (0),
`AutoboxingStateValueProperty` (11), `HardcodedText` (0 na Compose),
`SetTextI18n` (0). Ostale ugrađene u izveštaju (UnusedResources, UseKtx,
GradleDependency, ObsoleteSdkInt, …) nisu tematski vezane za naša 4 pravila.

**Zaključak merenja (bez ocene):** pravilo 2 (kolekcije) se 1:1 preklapa sa
Slack `ComposeUnstableCollections`; pravilo 1 (stanje) se na ovom komitu slaže
na nuli sa Slack i ugrađenim; pravila 3 (hardkod) i 1B (skupa alokacija) nemaju
ekvivalent u pregledanim skupovima.
