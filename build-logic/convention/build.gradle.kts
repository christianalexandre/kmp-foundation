plugins {
    `kotlin-dsl`
    // Os convention plugins também são código Kotlin, e são o código mais
    // delicado do repositório. O `detekt` daqui é alcançado pelo `check` da
    // build raiz — ver o build.gradle.kts da raiz.
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    detektPlugins(libs.detekt.formatting)
}

detekt {
    buildUponDefaultConfig = true
    // `rootProject` aqui é a build `build-logic`; o detekt.yml é o da raiz.
    config.setFrom(rootProject.file("../detekt.yml"))
    source.setFrom(files("src"))
    failOnSeverity = dev.detekt.gradle.extensions.FailOnSeverity.Info
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = "17"
}

gradlePlugin {
    plugins {
        register("kmpModule") {
            id = "kmpfoundation.kmp.module"
            implementationClass = "KmpModuleConventionPlugin"
        }
        register("androidApp") {
            id = "kmpfoundation.android.app"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("quality") {
            id = "kmpfoundation.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
