package com.kmpfoundation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.features.devices.ui.DeviceListScreen
import com.kmpfoundation.features.devices.ui.DeviceSettingsScreen
import com.kmpfoundation.nav.NavigationPolicy
import com.kmpfoundation.nav.Route
import com.kmpfoundation.nav.titleOf

/**
 * Desenha **um** destino — não o app inteiro.
 *
 * Cada raiz Compose (um `ComposeUIViewController` no iOS, uma Activity no Android)
 * chama isto com a rota que lhe coube. Não há `NavHost`, não há pilha aqui: a
 * pilha é a da plataforma, e esta função nem sabe que existem outras telas.
 *
 * Faz duas coisas, e só elas: a **moldura** comum a todo destino (tema, insets,
 * cabeçalho) e o **despacho** rota → tela. Nenhuma tela nasce aqui — toda tela
 * Compose mora em `features/<nome>/ui/`, e é essa a única resposta para "onde
 * ponho uma tela?" (SDD 03).
 *
 * Mora em `ui/` e não em `core/ui/` porque ligar rota a feature é papel do topo —
 * `core/` não pode conhecer feature nenhuma (SDD 02), e `nav/` só conhece ids.
 *
 * As telas recebem callbacks simples (`onDeviceSelected`), nunca rotas: quem
 * traduz intenção em destino é a [NavigationPolicy], para a decisão ficar testável
 * fora do Compose.
 *
 * @param onBack como sair deste destino, ou `null` quando ele é a **raiz da
 * pilha** — e aí não há cabeçalho, por decisão de produto: título só aparece
 * quando existe para onde voltar. Quem sabe se é raiz é a casca, que é dona da
 * pilha (ADR 0007); no Android é `isTaskRoot`, no iOS é o `rootViewController`.
 */
@Composable
fun RouteContent(
    route: Route,
    graph: AppGraph,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)?,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Insets tratados aqui, e não em cada casca: no iOS a nav bar do UIKit
            // fica escondida sempre (ver RootCoordinator), então as duas plataformas
            // desenham de borda a borda e cedem o mesmo espaço.
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                if (onBack != null) {
                    RouteHeader(title = titleOf(route), onBack = onBack)
                }

                Column(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING)) {
                    when (route) {
                        is Route.Devices -> DeviceListScreen(
                            viewModel = viewModel { graph.devices.listViewModel() },
                            onDeviceSelected = { onNavigate(NavigationPolicy.onDeviceSelected(it)) },
                            onDeviceSettings = { onNavigate(NavigationPolicy.onDeviceSettings(it)) },
                        )

                        is Route.DeviceSettings -> DeviceSettingsScreen(
                            viewModel = viewModel {
                                graph.devices.settingsViewModel(route.deviceId)
                            },
                        )

                        // Desenhada pela plataforma. A casca nunca chama RouteContent
                        // com ela; o ramo existe porque o `when` é exaustivo.
                        is Route.DeviceDetail -> Unit
                    }
                }
            }
        }
    }
}

/**
 * O cabeçalho é **desenhado aqui**, e não pela nav bar do UIKit, para as duas
 * plataformas terem o mesmo. O preço está registrado no RootCoordinator: sem a
 * barra, o voltar é nosso e o gesto de borda precisa ser reabilitado à mão.
 */
@Composable
private fun RouteHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Voltar") }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

private val CONTENT_PADDING = 24.dp
private val HEADER_HEIGHT = 56.dp
