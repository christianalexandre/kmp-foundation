package com.kmpfoundation.features.devices.data

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.core.platform.ScanEvent
import com.kmpfoundation.core.platform.scan
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlatformDeviceRepository(
    private val scanner: DeviceScanner,
) : DeviceRepository {
    override fun discover(): Flow<DiscoveryEvent> = scanner.scan().map { event ->
        when (event) {
            is ScanEvent.Found -> DiscoveryEvent.Found(Device(event.device.id, event.device.name))
            is ScanEvent.Failed -> DiscoveryEvent.Failed(event.error)
        }
    }
}
