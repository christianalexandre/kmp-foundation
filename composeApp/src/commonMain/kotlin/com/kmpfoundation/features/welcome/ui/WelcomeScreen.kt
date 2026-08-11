package com.kmpfoundation.features.welcome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kmpfoundation.features.welcome.presentation.WelcomeStateHolder
import com.kmpfoundation.features.welcome.presentation.WelcomeUiState

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/**
 * Casca fina: renderiza o estado e delega ações ao state holder. Pacotes
 * `*.ui.*` ficam fora do gate de cobertura — logo, nada de lógica aqui.
 */
@Composable
fun WelcomeScreen(stateHolder: WelcomeStateHolder) {
    val state by stateHolder.state.collectAsState()
    LaunchedEffect(stateHolder) { stateHolder.load() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is WelcomeUiState.Loading -> CircularProgressIndicator()
            is WelcomeUiState.Content -> Text(current.message)
        }
    }
}
