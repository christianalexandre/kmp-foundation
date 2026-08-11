// Raiz intencionalmente enxuta: targets, gates e configurações compartilhadas
// vivem nos convention plugins em build-logic/ e são aplicados por módulo.
plugins {
    // `base` só para a raiz ter um `check` onde pendurar o lint do build-logic.
    base
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt) apply false
}

// O build-logic é uma included build: as tasks dele não entram no `check` da
// raiz sozinhas. Sem esta ponte, os convention plugins ficariam fora do lint.
val detektBuildLogic by tasks.registering {
    group = "verification"
    description = "Runs Detekt over the convention plugins in build-logic/."
    dependsOn(gradle.includedBuild("build-logic").task(":convention:detekt"))
}

tasks.named("check").configure {
    dependsOn(detektBuildLogic)
}
