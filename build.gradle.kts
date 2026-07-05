// Koreni build fajl - pluginovi se deklarišu ovde (apply false),
// a primenjuju u pojedinačnim modulima.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}
