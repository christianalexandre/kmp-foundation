package com.kmpfoundation.features.devices.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpfoundation.features.devices.presentation.DeviceSettingsUiState
import com.kmpfoundation.features.devices.presentation.DeviceSettingsViewModel

/**
 * Tela Compose com **duas entradas**: da lista (Compose → Compose) e da tela
 * nativa (Compose por cima do nativo). Ela não sabe por qual veio, e é esse o
 * ponto — o destino é o mesmo, a raiz Compose é nova nos dois casos.
 *
 * Mora aqui, em `features/devices/ui/`, como toda tela Compose. Ela já morou
 * dentro do `RouteContent`, e isso dava duas respostas à pergunta "onde ponho uma
 * tela?" — o `RouteContent` só despacha rota para tela (SDD 03).
 */
@Composable
fun DeviceSettingsScreen(viewModel: DeviceSettingsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (val current = state) {
            is DeviceSettingsUiState.Loading -> CircularProgressIndicator()

            is DeviceSettingsUiState.Content -> {
                Text(current.device.name, style = MaterialTheme.typography.titleMedium)
                Text("id ${current.device.id}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "O nome não veio na rota — ela carrega só o id (ADR 0007). Esta " +
                        "tela o buscou no repositório, que vive no escopo de aplicação " +
                        "e por isso atravessa as raízes Compose.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is DeviceSettingsUiState.Error -> Text(current.message)
        }
    }
}
