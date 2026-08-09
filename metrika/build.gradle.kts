// metrika: čist Kotlin/JVM modul (BEZ Android zavisnosti), izvršiv kao CLI.
// Metrički sloj faze 3: parsira lint XML izveštaj + broji izvorni kod ->
// jedan red normalizovane metrike tehničkog duga (CSV/JSON) + trend HTML.
//
// Namerno bez spoljnih zavisnosti: XML se parsira JDK-ovim javax.xml,
// JSON/CSV/HTML se pišu ručno. Time modul ostaje lako gradiv i u CI-ju.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

application {
    // Ulazna tačka CLI-ja (top-level fun main u Cli.kt -> klasa CliKt).
    mainClass.set("rs.diplomski.metrika.CliKt")
}
