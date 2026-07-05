// sample-app: minimalna Compose aplikacija koja služi kao
// poligon za isprobavanje lint pravila.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "rs.diplomski.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "rs.diplomski.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // Kapija: prekršaj nivoa error obara build (Faza 1, korak 5
        // se oslanja na ovo da bi CI "pukao").
        abortOnError = true

        // Izveštaji koje će Faza 3 (metrički sloj) da parsira.
        xmlReport = true
        htmlReport = true
        textReport = true
    }
}

dependencies {
    // KLJUČNA LINIJA: ovim se naša custom pravila vezuju za aplikaciju.
    // lintChecks (a ne lintPublish) jer je modul lokalni, u istom projektu.
    lintChecks(project(":lint-rules"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
