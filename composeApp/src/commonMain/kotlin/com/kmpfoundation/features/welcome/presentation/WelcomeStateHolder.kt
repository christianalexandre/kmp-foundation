package com.kmpfoundation.features.welcome.presentation

import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/** Estado imutável observado pela UI. */
sealed interface WelcomeUiState {
    data object Loading : WelcomeUiState
    data class Content(val message: String) : WelcomeUiState
}

/**
 * State holder: TODA a lógica de apresentação vive aqui (dentro do gate de
 * cobertura), nunca no composable. O escopo vem de fora — a UI passa o dela,
 * os testes passam o do runTest.
 */
class WelcomeStateHolder(
    private val getWelcomeMessage: GetWelcomeMessageUseCase,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun load() {
        _state.value = WelcomeUiState.Loading
        scope.launch {
            _state.value = WelcomeUiState.Content(getWelcomeMessage().text)
        }
    }
}
