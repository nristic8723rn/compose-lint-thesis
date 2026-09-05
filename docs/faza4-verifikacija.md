# Faza 4b — Zadatak 1: verifikacija pravila 1 (StanjeBezRemember)

**Pitanje:** pravilo `StanjeBezRemember` daje 0 nalaza na 5 realnih projekata.
Da li je to NALAZ (residual iz 2c — bytecode composable lambde) ili BUG (pravilo
ne radi na realnom kodu, a stubovi u testovima to sakrivaju)?

**Metod:** u kloniran Feeder (`spacecowboy/Feeder` @ `328ffdee`, newest) svesno
ubačena tri prekršaja u jedan @Composable fajl, pa pušten lint sa našim jar-om
(lint-api 31.7.3). Feeder-ov `lint { ignoreWarnings = true }` privremeno ugašen
samo za merenje (case c je WARNING). Feeder vraćen u čisto stanje posle merenja.

## Ubačeni kod (`.../feeder/verifikacija/VerifikacijaTest.kt`)

```kotlin
@Composable
fun VerifikacijaA() {
    // (a) direktno u telu @Composable, van svake lambde
    val brojac = mutableStateOf(0)        // linija 13
    println(brojac)
}

@Composable
fun VerifikacijaB() {
    Column {                              // bibliotečki (bytecode) Column
        val brojac = mutableStateOf(0)    // linija 21
        println(brojac)
    }
}

@Composable
fun VerifikacijaC() {
    val obrazac = Regex("[a-z]+")         // linija 29
    println(obrazac)
}
```

## Izmereni ishod (iz lint XML izveštaja)

| Slučaj | Linija | NAŠE pravilo | Google ugrađeno |
|---|---|---|---|
| (a) mutableStateOf direktno u telu | 13 | **StanjeBezRemember — PRIJAVLJENO** ✓ | UnrememberedMutableState, AutoboxingStateCreation |
| (b) mutableStateOf u bibliotečkom `Column { }` | 21 | **ćuti** (nema StanjeBezRemember) | UnrememberedMutableState, AutoboxingStateCreation |
| (c) Regex direktno u telu | 29 | **SkupaAlokacijaBezRemember — PRIJAVLJENO** ✓ | — |

Sirovi ID+linija po VerifikacijaTest.kt (iz `lint-results-fdroidDebug.xml`):
```
StanjeBezRemember           line=13
SkupaAlokacijaBezRemember   line=29
UnrememberedMutableState    line=13   (Google)
UnrememberedMutableState    line=21   (Google)
AutoboxingStateCreation     line=13   (Google)
AutoboxingStateCreation     line=21   (Google)
```

## Zaključak

**Hipoteza POTVRĐENA; pravilo 1 NIJE bug.**

- (a) i (c) pucaju kad je prekršaj DIREKTNO u telu @Composable funkcije — pravilo
  radi na realnom kodu, ne samo na test-stubovima.
- (b) ćuti kad je isti poziv unutar bibliotečkog `Column { }` — to je residual
  izmeren u fazi 2c: type-use `@Composable` na funkcijskom tipu iz bytecode-a se
  ne vidi kroz `param.type.annotations`, pa se ne penjemo kroz tu lambdu.
- Isti slučaj (b) Google-ov ugrađeni `UnrememberedMutableState` HVATA (line 21) —
  direktan, izmeren dokaz da za taj procep ugrađeno pravilo ima bolji doseg.

**Posledica za evaluaciju:** nula nalaza pravila 1 na realnim projektima je
očekivana i objašnjena — realan Compose kod stanje gotovo uvek kreira unutar
bibliotečkih composable lambdi (Column/Row/Box/LazyColumn…), gde naše pravilo
ćuti. Trend u Fazi 4 nose pravila 2 i 3; za pravilo 1 se eksplicitno navodi
izmerena granica (nalaz, ne rupa).
