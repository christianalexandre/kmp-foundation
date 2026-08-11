plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
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
