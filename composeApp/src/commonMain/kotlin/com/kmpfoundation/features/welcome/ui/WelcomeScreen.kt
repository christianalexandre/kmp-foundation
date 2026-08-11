package com.kmpfoundation.features.welcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpfoundation.features.welcome.presentation.WelcomeUiState
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel) {
    // `WithLifecycle` e não `collectAsState`: sem isso a tela segue coletando
    // com o app em background. A carga inicial é do ViewModel, não daqui.
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val current = state) {
            is WelcomeUiState.Loading -> CircularProgressIndicator()

            is WelcomeUiState.Content -> Text(current.message)

            is WelcomeUiState.Error -> {
                Text(current.message)
                if (current.canRetry) {
                    TextButton(onClick = viewModel::load) { Text("Tentar de novo") }
                }
            }
        }
    }
}
