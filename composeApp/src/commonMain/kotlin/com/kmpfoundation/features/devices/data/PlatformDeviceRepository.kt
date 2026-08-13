package com.kmpfoundation.features.devices.data

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.core.platform.ScanEvent
import com.kmpfoundation.core.platform.scan
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DeviceRepository
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Guarda o que a busca encontrou, e é isso que faz a tela de ajustes funcionar.
 *
 * O motivo é o ADR 0007: com **uma raiz Compose por destino**, a tela de ajustes
 * abre numa composição nova e vazia — não há como ela receber o `Device` que a
 * lista já tinha em memória, nem quando vem da tela nativa no meio do caminho.
 * O ADR registra essa consequência como obrigatória: estado compartilhado entre
 * telas não cabe mais na composição, precisa de um dono que viva mais que ela.
 *
 * Aqui esse dono é este repositório, que o `AppGraph` cria uma vez e mantém no
 * escopo de aplicação (ADR 0003). Num app real o mesmo papel seria de um cache em
 * disco ou banco: depois de morte de processo este mapa está vazio, e os ajustes
 * abrem em `NotFound` — comportamento correto, e a razão de o ADR 0007 pedir um
 * repositório **durável** para o resultado que vem de tela nativa.
 *
 * Sem sincronização: escrita e leitura acontecem no `viewModelScope`, ou seja, na
 * main. É o mesmo contrato de entrega serializada que a engine já honra (ADR 0004).
 */
class PlatformDeviceRepository(
    private val scanner: DeviceScanner,
) : DeviceRepository {

    private val found = mutableMapOf<String, Device>()

    override fun discover(): Flow<DiscoveryEvent> = scanner.scan().map { event ->
        when (event) {
            is ScanEvent.Found -> {
                val device = Device(event.device.id, event.device.name)
                found[device.id] = device
                DiscoveryEvent.Found(device)
            }

            is ScanEvent.Failed -> DiscoveryEvent.Failed(event.error)
        }
    }

    override suspend fun device(id: String): Outcome<Device> =
        found[id]?.let { Outcome.Success(it) } ?: Outcome.Failure(AppError.NotFound)
}
