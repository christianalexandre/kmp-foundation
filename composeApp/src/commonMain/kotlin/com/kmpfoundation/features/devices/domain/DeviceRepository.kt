package com.kmpfoundation.features.devices.domain

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    /**
     * Fluxo frio: a busca começa quando alguém coleta e **para quando a coleta
     * para**. Cancelar o coletor é o que desliga o recurso nativo.
     */
    fun discover(): Flow<DiscoveryEvent>

    /**
     * Um dispositivo pelo id — operação **única**, e por isso devolve [Outcome]
     * em vez de um fluxo de eventos (ADR 0002).
     *
     * Existe porque a rota carrega **id, não objeto** (ADR 0007): quem abre a tela
     * de ajustes tem só o id na mão e precisa perguntar quem é. É `suspend` porque
     * num app real esta pergunta vai a disco ou à rede; aqui responde de memória.
     */
    suspend fun device(id: String): Outcome<Device>
}

sealed interface DiscoveryEvent {
    data class Found(val device: Device) : DiscoveryEvent
    data class Failed(val error: AppError) : DiscoveryEvent
}
