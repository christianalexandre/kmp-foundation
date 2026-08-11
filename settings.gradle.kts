rootProject.name = "kmp-foundation"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Convention plugins do projeto (quality gates, targets KMP).
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Lógica e UI compartilhadas (KMP: Android library + iOS framework).
include(":composeApp")
// Casca Android que empacota o módulo compartilhado. Separado por exigência do
// AGP 9: um módulo KMP não pode mais ser `com.android.application`.
include(":androidApp")
