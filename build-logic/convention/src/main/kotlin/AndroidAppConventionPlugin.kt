import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure

/**
 * App Android: a casca que empacota a library KMP compartilhada.
 *
 * Só lint aqui (Detekt) — cobertura não se aplica a um módulo cujo conteúdo é
 * `MainActivity`; a lógica testável vive no módulo compartilhado.
 *
 * Com o AGP 9 o plugin Kotlin é embutido (`android.builtInKotlin`), então este
 * módulo não aplica `org.jetbrains.kotlin.android`.
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        fun version(name: String) = libs.findVersion(name).get().requiredVersion.toInt()

        extensions.configure<ApplicationExtension> {
            compileSdk = version("androidCompileSdk")
            defaultConfig {
                minSdk = version("androidMinSdk")
                targetSdk = version("androidTargetSdk")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        configureDetekt()
    }
}
