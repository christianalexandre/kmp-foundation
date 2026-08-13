package com.kmpfoundation.features.devices.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpfoundation.features.devices.presentation.DeviceListViewModel

/**
 * Cada linha abre **dois tipos de destino**: o nome leva à tela nativa, "Ajustes"
 * leva a uma tela Compose. É o mesmo gesto de navegar nos dois casos — a tela não
 * sabe a diferença, e quem sabe é a casca, por `Route.isNative`.
 */
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel,
    onDeviceSelected: (String) -> Unit,
    onDeviceSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
        ) {
            Text(if (state.isScanning) "Parar busca" else "Buscar dispositivos")
        }

        // Nível 1 da fronteira nativa (`expect/actual`) ao lado do nível 3 (a engine
        // que faz a busca): a casca que implementa uma é a mesma que implementa a
        // outra. Ver SDD 03.
        Text(
            text = "Busca fornecida pela casca — ${viewModel.platform}",
            style = MaterialTheme.typography.bodySmall,
        )

        state.errorMessage?.let { Text(it) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.devices, key = { it.id }) { device ->
                // As telas recebem callbacks simples, nunca rotas: quem traduz
                // intenção em destino é a NavigationPolicy — ADR 0007.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = device.name,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDeviceSelected(device.id) }
                            .padding(vertical = 8.dp),
                    )
                    TextButton(onClick = { onDeviceSettings(device.id) }) { Text("Ajustes") }
                }
            }
        }
    }
}
