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
    implementation(libs.androidx.activity.compose)
}
