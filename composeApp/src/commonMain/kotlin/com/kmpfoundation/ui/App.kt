package com.kmpfoundation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.features.devices.ui.DeviceListScreen
import com.kmpfoundation.features.welcome.ui.WelcomeScreen

// Mora em `ui/` e não em `core/ui/` de propósito: compor features é papel do
// topo, e `core/` não pode conhecer feature nenhuma (style guide, SDD 02).
// Vira a casca de navegação quando ela existir.
@Composable
fun App(graph: AppGraph) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            WelcomeScreen(viewModel { graph.welcome.viewModel() })
            DeviceListScreen(viewModel { graph.devices.viewModel() })
        }
    }
}
