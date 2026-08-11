// Raiz intencionalmente enxuta: targets, gates e configurações compartilhadas
// vivem nos convention plugins em build-logic/ e são aplicados por módulo.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt) apply false
}
