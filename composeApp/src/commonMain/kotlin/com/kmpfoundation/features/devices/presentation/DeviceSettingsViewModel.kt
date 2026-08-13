package com.kmpfoundation.features.devices.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DeviceSettingsUiState {
    data object Loading : DeviceSettingsUiState
    data class Content(val device: Device) : DeviceSettingsUiState
    data class Error(val message: String) : DeviceSettingsUiState
}

/**
 * O par do [DeviceListViewModel] na mesma feature, e de propósito: a lista é o
 * caso de **fluxo com cancelamento** (ADR 0004), este é o de **operação única com
 * resultado tipado** (ADR 0002). São os dois formatos que um app real usa.
 *
 * Recebe `deviceId` e não `Device` porque foi isso que a rota trouxe (ADR 0007).
 */
class DeviceSettingsViewModel(
    private val deviceId: String,
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<DeviceSettingsUiState>(DeviceSettingsUiState.Loading)
    val state: StateFlow<DeviceSettingsUiState> = _state.asStateFlow()

    // Carga inicial aqui, não num LaunchedEffect da tela — style guide, "Telas".
    init {
        viewModelScope.launch {
            _state.value = when (val outcome = repository.device(deviceId)) {
                is Outcome.Success -> DeviceSettingsUiState.Content(outcome.value)
                is Outcome.Failure -> DeviceSettingsUiState.Error(outcome.error.toMessage())
            }
        }
    }
}

// Erro vira texto aqui, nunca na tela — ADR 0002. Os textos são desta tela, e não
// de um mapa comum: "não encontrado" na busca e "não encontrado" nos ajustes pedem
// saídas diferentes ao usuário.
private fun AppError.toMessage(): String = when (this) {
    is AppError.NotFound -> "Dispositivo não encontrado. Volte e busque de novo."
    is AppError.Connectivity -> "Sem conexão. Verifique a internet."
    is AppError.Unauthorized -> "Sua sessão expirou. Entre novamente."
    is AppError.Platform -> "Ajustes indisponíveis neste aparelho."
    is AppError.Unexpected -> "Algo deu errado ao abrir os ajustes."
}
