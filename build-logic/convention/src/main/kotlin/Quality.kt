import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal const val MIN_LINE_COVERAGE_PERCENT = 80

/**
 * Detekt + ruleset de formatação (ktlint embutido). Config em `detekt.yml` na
 * raiz; qualquer issue falha o build. Aplicado a todo módulo Kotlin — inclusive
 * ao app Android, que não tem cobertura mas tem código a lintar.
 */
internal fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("detekt.yml"))
        // Sem type resolution: a task `detekt` varre todos os source sets KMP.
        source.setFrom(files("src"))
        // Gate rígido: qualquer achado quebra o build (o antigo `maxIssues: 0`,
        // que saiu do detekt.yml no Detekt 2).
        failOnSeverity.set(FailOnSeverity.Info)
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget.set("17")
        exclude("**/build/**")
    }

    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    dependencies {
        add("detektPlugins", libs.findLibrary("detekt-formatting").get())
    }

    tasks.named("check").configure {
        dependsOn("detekt")
    }
}

/**
 * Kover: cobertura de LINHA mínima de 80%. Pacotes `*.ui.*` (composables) e
 * entry points de plataforma ficam fora da medição — a regra que sustenta o
 * filtro está no style guide: lógica não vive em `ui/`.
 */
internal fun Project.configureKover() {
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    classes(
                        // UI declarativa: verificada por review/snapshot, não por unit test.
                        "*.ui.*",
                        // Classes geradas pelo compilador do Compose para lambdas @Composable.
                        "*.ComposableSingletons*",
                        // Entry points de plataforma.
                        "*.MainActivity",
                        "*.MainActivityKt",
                        "*.MainViewControllerKt",
                    )
                }
            }
            total {
                verify {
                    rule("Cobertura mínima de linha: $MIN_LINE_COVERAGE_PERCENT%") {
                        minBound(MIN_LINE_COVERAGE_PERCENT)
                    }
                }
            }
        }
    }

    tasks.named("check").configure {
        dependsOn("koverVerify")
    }
}
