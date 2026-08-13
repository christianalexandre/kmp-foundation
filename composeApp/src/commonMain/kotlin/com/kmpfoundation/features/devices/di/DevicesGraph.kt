package com.kmpfoundation.features.devices.di

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.features.devices.data.PlatformDeviceRepository
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.presentation.DeviceListViewModel
import com.kmpfoundation.features.devices.presentation.DeviceSettingsViewModel

/**
 * Feature de exemplo: apagar a pasta `features/devices` ao clonar o template.
 *
 * O repositório é criado **uma vez**, aqui, e o `AppGraph` mantém este grafo pelo
 * processo inteiro (ADR 0003). É isso que dá à tela de ajustes acesso ao que a
 * busca encontrou, mesmo abrindo numa raiz Compose nova — ver
 * [PlatformDeviceRepository].
 *
 * ViewModel, ao contrário, sai por **factory**: quem é dono do ciclo de vida dele
 * é o `viewModel { }` da tela, nunca o grafo.
 */
class DevicesGraph(
    private val deviceRepository: DeviceRepository,
) {
    constructor(scanner: DeviceScanner) : this(PlatformDeviceRepository(scanner))

    fun listViewModel(): DeviceListViewModel = DeviceListViewModel(deviceRepository)

    fun settingsViewModel(deviceId: String): DeviceSettingsViewModel =
        DeviceSettingsViewModel(deviceId, deviceRepository)
}
