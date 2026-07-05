// lint-rules: čist Kotlin/JVM modul (NIJE Android modul).
// Lint pravila se izvršavaju na JVM-u unutar build procesa,
// pa im Android SDK nije potreban kao zavisnost.
plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // compileOnly: lint API obezbeđuje AGP u runtime-u,
    // ne pakujemo ga u naš jar.
    compileOnly(libs.lint.api)

    // Test harnisa za lint pravila (LintDetectorTest).
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.core)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.jar {
    manifest {
        // KLJUČNA LINIJA: bez ovog atributa lint uopšte ne zna
        // da naš jar sadrži registar pravila. Vrednost mora biti
        // pun naziv klase koja nasleđuje IssueRegistry.
        attributes("Lint-Registry-v2" to "rs.diplomski.lint.ComposeIssueRegistry")
    }
}
