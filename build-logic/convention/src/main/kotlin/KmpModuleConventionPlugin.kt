import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Módulo KMP do projeto: Android (como library) + iOS arm64/simulador. */
class KmpModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        fun version(name: String) = libs.findVersion(name).get().requiredVersion.toInt()

        extensions.configure<KotlinMultiplatformExtension> {
            // No AGP 9.3 o bloco chama-se `android` (o `androidLibrary` da
            // documentação inicial já nasceu deprecado).
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>(
                "android",
            ) {
                compileSdk = version("androidCompileSdk")
                minSdk = version("androidMinSdk")
                // Cria a compilation de testes unitários JVM (o antigo
                // androidUnitTest); sem isso o commonTest não roda no Android.
                withHostTest { }
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            // iosX64 (simulador Intel) fora: o CMP 1.11+ não publica mais esse
            // alvo (mesma decisão do imou-smart-home).
            iosArm64()
            iosSimulatorArm64()
        }
    }
}
