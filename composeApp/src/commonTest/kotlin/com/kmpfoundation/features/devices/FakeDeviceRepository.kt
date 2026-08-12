package com.kmpfoundation.features.devices

import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeDeviceRepository(
    private val events: List<DiscoveryEvent> = emptyList(),
) : DeviceRepository {
    override fun discover(): Flow<DiscoveryEvent> = flowOf(*events.toTypedArray())
}
