package com.kmpfoundation.features.devices

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeDeviceRepository(
    private val events: List<DiscoveryEvent> = emptyList(),
    /** O resultado fixo de [device] — inclusive a falha, para exercitar cada `AppError`. */
    private val lookup: Outcome<Device> = Outcome.Failure(AppError.NotFound),
) : DeviceRepository {
    override fun discover(): Flow<DiscoveryEvent> = flowOf(*events.toTypedArray())

    override suspend fun device(id: String): Outcome<Device> = lookup
}
