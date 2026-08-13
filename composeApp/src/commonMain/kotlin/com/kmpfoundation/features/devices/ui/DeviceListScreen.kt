package com.kmpfoundation.features.devices.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpfoundation.features.devices.presentation.DeviceListViewModel

@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel,
    onDeviceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
        ) {
            Text(if (state.isScanning) "Parar busca" else "Buscar dispositivos")
        }

        state.errorMessage?.let { Text(it) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.devices, key = { it.id }) { device ->
                // Recebe um callback simples, não uma rota: a tela não conhece
                // navegação, e a decisão fica na NavigationPolicy — ADR 0007.
                Text(
                    text = device.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device.id) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}
