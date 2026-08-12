package com.kmpfoundation.features.devices.di

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.features.devices.data.PlatformDeviceRepository
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.presentation.DeviceListViewModel

// Feature de exemplo: apagar a pasta `features/devices` ao clonar o template.
class DevicesGraph(
    private val deviceRepository: DeviceRepository,
) {
    constructor(scanner: DeviceScanner) : this(PlatformDeviceRepository(scanner))

    fun viewModel(): DeviceListViewModel = DeviceListViewModel(deviceRepository)
}
