plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Rotas serializáveis: é o que atravessa o extra do Intent no Android e o que
    // torna deeplink e restauração possíveis — ADR 0007.
    alias(libs.plugins.kotlinSerialization)
    // Targets KMP (Android library + iOS) e quality gates (Kover 80% + Detekt)
    // vêm dos convention plugins — ver build-logic/convention/src/main/kotlin/.
    id("kmpfoundation.kmp.module")
    id("kmpfoundation.quality")
}

kotlin {
    // O namespace é por módulo; compileSdk/minSdk vêm do convention plugin.
    android {
        namespace = "com.kmpfoundation.shared"
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
