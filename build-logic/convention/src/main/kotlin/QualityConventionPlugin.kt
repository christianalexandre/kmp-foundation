import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Quality gates completos (cobertura + análise estática), amarrados ao `check`.
 * Para módulos que só precisam de lint (o app Android, que é casca), use o
 * `kmpfoundation.android.app`, que já aplica o Detekt sozinho.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureKover()
        configureDetekt()
    }
}
