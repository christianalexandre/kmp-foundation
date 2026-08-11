import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Quality gates locais do projeto, amarrados ao ciclo `check`:
 *
 * - Kover: cobertura de LINHA mínima de 80%. Pacotes `*.ui.*` (composables) e
 *   entry points de plataforma ficam fora da medição — a regra que sustenta o
 *   filtro está no style guide: lógica não vive em `ui/`.
 * - Detekt: análise estática + formatação (ruleset detekt-formatting embute o
 *   ktlint). Config em `detekt.yml` na raiz; qualquer issue falha o build.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        extensions.configure(KoverProjectExtension::class.java) {
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
                        rule("Cobertura mínima de linha: 80%") {
                            minBound(MIN_LINE_COVERAGE_PERCENT)
                        }
                    }
                }
            }
        }

        extensions.configure(DetektExtension::class.java) {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("detekt.yml"))
            // Sem type resolution: a task `detekt` varre todos os source sets KMP.
            source.setFrom(files("src"))
        }
        tasks.withType(Detekt::class.java).configureEach {
            jvmTarget = "17"
            exclude("**/build/**")
        }

        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        dependencies.add(
            "detektPlugins",
            libs.findLibrary("detekt-formatting").get(),
        )

        // Ambos os plugins já se penduram no `check`; o dependsOn explícito
        // documenta e garante o contrato do gate local.
        tasks.named("check").configure {
            dependsOn("koverVerify", "detekt")
        }
    }

    private companion object {
        const val MIN_LINE_COVERAGE_PERCENT = 80
    }
}
