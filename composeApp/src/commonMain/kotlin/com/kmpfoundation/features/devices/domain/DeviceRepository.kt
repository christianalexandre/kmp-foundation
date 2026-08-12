package com.kmpfoundation.features.devices.domain

import com.kmpfoundation.core.result.AppError
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    /**
     * Fluxo frio: a busca começa quando alguém coleta e **para quando a coleta
     * para**. Cancelar o coletor é o que desliga o recurso nativo.
     */
    fun discover(): Flow<DiscoveryEvent>
}

sealed interface DiscoveryEvent {
    data class Found(val device: Device) : DiscoveryEvent
    data class Failed(val error: AppError) : DiscoveryEvent
}
