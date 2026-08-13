plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Config do app (SDK levels, Java 17) + Detekt vêm do convention plugin.
    id("kmpfoundation.android.app")
}

android {
    namespace = "com.kmpfoundation.androidapp"

    defaultConfig {
        applicationId = "com.kmpfoundation"
        versionCode = 1
        versionName = "0.1.0"
    }

}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.compose.runtime)
    // A casca precisa de layout para consumir insets: com targetSdk 35+ o sistema
    // força edge-to-edge, e quem hospeda o Compose é quem trata as barras.
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
}
