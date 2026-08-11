package com.kmpfoundation.features.welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WelcomeUiState {
    data object Loading : WelcomeUiState
    data class Content(val message: String) : WelcomeUiState
}

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
