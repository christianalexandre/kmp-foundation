package com.kmpfoundation.features.devices.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpfoundation.core.platform.platformName
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<Device> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
)

class DeviceListViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DeviceListUiState())
    val state: StateFlow<DeviceListUiState> = _state.asStateFlow()

    /**
     * Nível 1 da fronteira nativa — `expect/actual`, sem interface nem callback,
     * porque o Kotlin/Native alcança a API sozinho (SDD 03). Fica fora do
     * `state` de propósito: não muda, e `state` é para o que muda.
     */
    val platform: String = platformName()

    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _state.value = DeviceListUiState(isScanning = true)
        scanJob = viewModelScope.launch {
            repository.discover().collect { event ->
                _state.value = when (event) {
                    is DiscoveryEvent.Found ->
                        _state.value.copy(devices = _state.value.devices + event.device)

                    is DiscoveryEvent.Failed ->
                        _state.value.copy(errorMessage = event.error.toMessage())
                }
            }
            // A coleta terminou sozinha (a busca acabou), não foi cancelada.
            _state.value = _state.value.copy(isScanning = false)
        }
    }

    /**
     * Cancelar o job encerra a coleta, o que dispara o `awaitClose` do
     * `callbackFlow` e, por ele, o `cancel()` do handle nativo. É esta cadeia
     * que faz o rádio/câmera desligar de verdade.
     *
     * O mesmo acontece sem ninguém chamar nada quando o ViewModel é destruído:
     * o `viewModelScope` cancela, e a cadeia corre igual.
     */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }
}

private fun AppError.toMessage(): String = when (this) {
    is AppError.Connectivity -> "Sem conexão. Verifique a internet."
    is AppError.NotFound -> "Nada encontrado."
    is AppError.Unauthorized -> "Sua sessão expirou. Entre novamente."
    is AppError.Platform -> "Busca indisponível neste aparelho."
    is AppError.Unexpected -> "Algo deu errado durante a busca."
}
