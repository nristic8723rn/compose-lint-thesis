# Arhitektura okvira za statičku analizu Compose koda

Ovaj dokument prikazuje ceo sistem od pisanja koda do izveštaja za menadžment.
Dijagram je u Mermaid formatu (renderuje se na GitHubu i lako se pretače u sliku
za rad); ispod njega je opis svakog koraka punim rečenicama.

## Dijagram toka

```mermaid
flowchart TD
    subgraph LOK["1. Lokalni razvoj"]
      A["Pisanje Compose koda"] --> B["IDE podvlači prekršaje<br/>custom lint pravila (modul lint-rules)"]
      B --> C["git commit + push na granu"]
    end

    subgraph CI["2. CI — GitHub Actions"]
      D["Testovi pravila<br/>lint-rules:test"]
      E["sample-app:lintDebug<br/>lint na aplikaciji"]
    end

    subgraph MET["4. Metrički sloj — modul metrika"]
      P["Parser + normalizatori (KLOC, @Composable)<br/>+ ponderi po merilu"] --> Q["Merna tačka<br/>CSV red + JSON, po projektu"]
      Q --> R["CSV istorija<br/>merne tačke kroz vreme"]
      R --> S["Trend izveštaj HTML<br/>offline, za menadžment"]
    end

    C --> U["Otvaranje / ažuriranje PR-a"]
    U --> D
    U --> E
    D --> F{"3. Kapija kvaliteta"}
    E --> F
    F -->|"ERROR: StanjeBezRemember"| G["PR BLOKIRAN<br/>branch protection na main"]
    F -->|"WARNING: ostala 3 pravila"| H["PR prolazi<br/>dug se samo prijavljuje"]
    E --> X["lint XML izveštaj<br/>artefakt, čuva se i kad build padne"]
    X --> P

    classDef blok fill:#fde8e8,stroke:#c0392b;
    classDef upoz fill:#fff4e5,stroke:#e67e22;
    class G blok;
    class H upoz;
```

## Opis koraka

### 1. Lokalni razvoj (IDE)

Programer piše Compose kod u Android Studiju. Custom lint pravila iz modula
`lint-rules` vezana su za aplikaciju kroz `lintChecks(project(":lint-rules"))`,
pa IDE podvlači prekršaje uživo — crvenom linijom za greške (nivo `error`) i
žutom za upozorenja. Povratna sprega je trenutna: prekršaj se vidi pre komita,
što je najjeftinije mesto da se dug uhvati.

### 2. CI — GitHub Actions

Na svaki push/PR pokreće se `.github/workflows/lint.yml`, koji radi dve stvari:
pušta **testove samih pravila** (`:lint-rules:test`) da bi se osiguralo da pravila
i dalje rade kako je specifikovano, i pušta **lint nad aplikacijom**
(`:sample-app:lintDebug`). Izveštaji se čuvaju kao artefakti čak i kad build
padne (`if: always()`), da bi se nalazi mogli pregledati.

### 3. Kapija kvaliteta (politika primene)

Rezultat lint-a prolazi kroz kapiju čija je politika izvedena iz jednog načela
(ozbiljnost prati pouzdanost detekcije, videti `docs/politika-primene.md`):
prekršaj nivoa **ERROR** (`StanjeBezRemember` — korektnost, pouzdana detekcija)
obara build i, uz branch protection na `main`, **blokira PR**; prekršaji nivoa
**WARNING** (ostala tri pravila) se samo prijavljuju i ne blokiraju spajanje.
Pri uvođenju u živ projekat koristi se baseline: zatečeni dug se prihvata, a
samo novi prekršaji obaraju kapiju.

### 4. Metrički sloj (modul `metrika`)

lint XML izveštaj (artefakt iz koraka 2) ulazi u modul `metrika`. Parser izdvaja
naša 4 nalaza (tuđa broji kao kontekst), normalizatori mere veličinu koda
(KLOC i broj `@Composable`), a ponderi po merilu (UTICAJ × POUZDANOST) daju
ponderisani zbir duga. Rezultat je jedna **merna tačka** (jedan CSV red + JSON),
vezana za projekat i komit. Merne tačke se dopisuju u **CSV istoriju**, iz koje
se generiše **trend izveštaj** (samostalan HTML, radi offline iz CI artefakta),
namenjen menadžmentu — sa razbijanjem po pravilu i po projektu.

## Granice i tok podataka

- **Jednosmeran tok:** kod → nalaz → metrika → izveštaj; metrički sloj ne menja
  kod niti pravila, samo meri.
- **Bez baze:** istorija je jedan CSV fajl (dopisivanje), što je dovoljno za
  trend i trivijalno za CI artefakt.
- **Razdvojene odgovornosti:** `lint-rules` (detekcija), `sample-app` (poligon),
  `metrika` (merenje i izveštavanje) su nezavisni Gradle moduli.
