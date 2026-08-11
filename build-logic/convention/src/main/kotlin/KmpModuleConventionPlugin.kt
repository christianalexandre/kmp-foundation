import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Targets KMP padrão do projeto: Android + iOS (arm64 device e simulador Apple
 * Silicon). Módulos aplicam este plugin DEPOIS do plugin Android
 * (com.android.application ou com.android.library), pois `androidTarget()`
 * exige um deles presente.
 */
class KmpModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")

        extensions.configure(KotlinMultiplatformExtension::class.java) {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
            // iosX64 (simulador Intel) fora: o CMP 1.11+ não publica mais esse
            // alvo (mesma decisão do imou-smart-home).
            iosArm64()
            iosSimulatorArm64()
        }

        extensions.configure(BaseExtension::class.java) {
            compileSdkVersion(35)
            defaultConfig.minSdk = 24
            defaultConfig.targetSdk = 35
            compileOptions.sourceCompatibility = JavaVersion.VERSION_17
            compileOptions.targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
