package com.kmpfoundation.features.welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WelcomeUiState {
    data object Loading : WelcomeUiState
    data class Content(val message: String) : WelcomeUiState
    data class Error(val message: String, val canRetry: Boolean) : WelcomeUiState
}

class WelcomeViewModel(
    private val getWelcomeMessage: GetWelcomeMessageUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun load() {
        _state.value = WelcomeUiState.Loading
        viewModelScope.launch {
            _state.value = when (val outcome = getWelcomeMessage()) {
                is Outcome.Success -> WelcomeUiState.Content(outcome.value.text)
                is Outcome.Failure -> outcome.error.toUiState()
            }
        }
    }
}

// O erro de domínio vira texto e ação AQUI, não na tela: é a decisão de
// produto sobre cada falha, e é o que os testes verificam.
private fun AppError.toUiState(): WelcomeUiState.Error = when (this) {
    is AppError.Connectivity ->
        WelcomeUiState.Error("Sem conexão. Verifique a internet.", canRetry = true)

    is AppError.NotFound ->
        WelcomeUiState.Error("Conteúdo indisponível.", canRetry = false)

    is AppError.Unauthorized ->
        WelcomeUiState.Error("Sua sessão expirou. Entre novamente.", canRetry = false)

    is AppError.Platform ->
        WelcomeUiState.Error("Recurso indisponível neste aparelho.", canRetry = false)

    is AppError.Unexpected ->
        WelcomeUiState.Error("Algo deu errado. Tente de novo.", canRetry = true)
}
