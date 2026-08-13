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

    buildTypes {
        release {
            // Existe para a bancada do RFC 0001 poder ser medida: em Debug o app é
            // `debuggable`, o que desliga otimizações da ART, e o número de 1º frame
            // sai enganoso — com aparência de confiável. Medição exige Release.
            //
            // Assinado com a chave de DEBUG de propósito: isto habilita medir em
            // device, não distribuir. Assinatura de verdade é assunto da dívida #7
            // (ambientes de build e segredos), ainda adiada.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
