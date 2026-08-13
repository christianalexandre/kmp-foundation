package com.kmpfoundation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.features.devices.ui.DeviceListScreen
import com.kmpfoundation.features.welcome.ui.WelcomeScreen
import com.kmpfoundation.nav.NavigationPolicy
import com.kmpfoundation.nav.Route

/**
 * Desenha **um** destino — não o app inteiro.
 *
 * Cada raiz Compose (um `ComposeUIViewController` no iOS, uma Activity no Android)
 * chama isto com a rota que lhe coube. Não há `NavHost`, não há pilha aqui: a
 * pilha é a da plataforma, e esta função nem sabe que existem outras telas.
 *
 * Mora em `ui/` e não em `core/ui/` porque ligar rota a feature é papel do topo —
 * `core/` não pode conhecer feature nenhuma (SDD 02), e `nav/` só conhece ids.
 *
 * As telas recebem callbacks simples (`onDeviceSelected`), nunca rotas: quem
 * traduz intenção em destino é a [NavigationPolicy], para a decisão ficar testável
 * fora do Compose.
 */
@Composable
fun RouteContent(
    route: Route,
    graph: AppGraph,
    onNavigate: (Route) -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (route) {
                is Route.Welcome -> Column(
                    modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING),
                    verticalArrangement = Arrangement.spacedBy(CONTENT_PADDING),
                ) {
                    WelcomeScreen(viewModel { graph.welcome.viewModel() })
                    Button(onClick = { onNavigate(NavigationPolicy.onSeeDevices()) }) {
                        Text("Ver dispositivos")
                    }
                }

                is Route.Devices -> Column(
                    modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING),
                ) {
                    DeviceListScreen(
                        viewModel = viewModel { graph.devices.viewModel() },
                        onDeviceSelected = { onNavigate(NavigationPolicy.onDeviceSelected(it)) },
                    )
                }

                is Route.DeviceSettings -> Column(
                    modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING),
                    verticalArrangement = Arrangement.spacedBy(CONTENT_PADDING),
                ) {
                    Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
                    Text("Dispositivo ${route.deviceId}")
                    Text(
                        "Esta tela foi aberta a partir de uma tela nativa — é o " +
                            "Compose de volta por cima dela.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Desenhada pela plataforma. A casca nunca chama RouteContent com ela;
                // o ramo existe porque o `when` é exaustivo.
                is Route.DeviceDetail -> Unit
            }
        }
    }
}

private val CONTENT_PADDING = 24.dp
