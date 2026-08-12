package com.kmpfoundation.features.welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.domain.MessageRepository
import kotlinx.coroutines.Job
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
    private val repository: MessageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    // Carga inicial aqui, não num LaunchedEffect da tela — style guide, "Telas".
    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        _state.value = WelcomeUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = when (val outcome = repository.welcomeMessage()) {
                is Outcome.Success -> WelcomeUiState.Content(outcome.value.text)
                is Outcome.Failure -> outcome.error.toUiState()
            }
        }
    }
}

// Erro vira texto e ação aqui, nunca na tela — ADR 0002.
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
