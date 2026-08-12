package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Caso **assíncrono** da fronteira nativa: trabalho que leva tempo, emite
 * eventos ao longo do caminho e **continua consumindo recurso enquanto ninguém
 * o parar**. É este cenário — e só ele — que justifica o [Cancellable]
 * (ADR 0004 e 0006). É a forma que uma busca BLE, um scan de rede ou um preview
 * de câmera vão ter.
 *
 * **Contrato que a implementação precisa honrar** (o adapter depende dele):
 * - callbacks entregues de forma **serializada**, na main thread;
 * - [DeviceScanCallback.onFinished] chamado **no máximo uma vez**;
 * - depois de [Cancellable.cancel], nenhum callback é entregue.
 *
 * O segundo item não é preciosismo: `resume` concorrente numa continuation é
 * `IllegalStateException`, e num Flow o `trySend` após `close` vira evento
 * perdido. Serializar na main é o jeito barato de garantir os dois.
 */
interface DeviceScanner {
    val isAvailable: Boolean

    fun startScan(callback: DeviceScanCallback): Cancellable
}

interface DeviceScanCallback {
    fun onDeviceFound(id: String, name: String)

    /** [errorMessage] preenchido quando a varredura terminou por falha. */
    fun onFinished(errorMessage: String?)
}

data class DiscoveredDevice(val id: String, val name: String)

sealed interface ScanEvent {
    data class Found(val device: DiscoveredDevice) : ScanEvent
    data class Failed(val error: AppError) : ScanEvent
}

/**
 * Adapter para o lado Kotlin. `callbackFlow` porque a varredura é um **fluxo**
 * de eventos, não um resultado único.
 *
 * O `awaitClose` é obrigatório e é o coração de tudo: quando o coletor some —
 * tela fechada, `viewModelScope` cancelado, `stopScan()` — é ele que desliga o
 * recurso nativo. Sem ele, o scan continuaria rodando com o app em background.
 */
fun DeviceScanner.scan(): Flow<ScanEvent> = callbackFlow {
    if (!isAvailable) {
        trySend(ScanEvent.Failed(AppError.Platform("busca indisponível nesta plataforma")))
        close()
        return@callbackFlow
    }

    val handle = startScan(
        object : DeviceScanCallback {
            override fun onDeviceFound(id: String, name: String) {
                trySend(ScanEvent.Found(DiscoveredDevice(id, name)))
            }

            override fun onFinished(errorMessage: String?) {
                if (errorMessage != null) {
                    trySend(ScanEvent.Failed(AppError.Platform(errorMessage)))
                }
                close()
            }
        },
    )

    awaitClose { handle.cancel() }
}
