package com.kmpfoundation.features.welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
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
 * ViewModel multiplataforma (JetBrains lifecycle): TODA a lógica de
 * apresentação vive aqui (dentro do gate de cobertura), nunca no composable.
 * No Android ele sobrevive à recriação da Activity; com a navegação futura,
 * fica escopado ao destino. Nos testes, o `viewModelScope` roda no dispatcher
 * de teste via `Dispatchers.setMain` — ver WelcomeViewModelTest.
 */
class WelcomeViewModel(
    private val getWelcomeMessage: GetWelcomeMessageUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun load() {
        _state.value = WelcomeUiState.Loading
        viewModelScope.launch {
            _state.value = WelcomeUiState.Content(getWelcomeMessage().text)
        }
    }
}
