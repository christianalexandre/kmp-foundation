package com.kmpfoundation.androidapp.platform

import android.os.Handler
import android.os.Looper
import com.kmpfoundation.core.platform.CallHandle
import com.kmpfoundation.core.platform.Cancellable
import com.kmpfoundation.core.platform.DeviceScanCallback
import com.kmpfoundation.core.platform.DeviceScanner

/**
 * Simula uma busca de dispositivos: acha um a cada [INTERVAL_MS] até
 * [MAX_DEVICES]. O SDK real (BLE, mDNS) entra exatamente aqui.
 *
 * A **tarefa repetida é recurso de verdade**: se ninguém cancelar, ela segue
 * agendando a si mesma. É o que torna este exemplo honesto — o `cancel()` tem
 * o que desligar.
 *
 * Repare que o trabalho não vai para a main thread por ser trabalho; num SDK
 * real ele roda nas threads do próprio SDK. O que o contrato exige é que a
 * **entrega do callback** seja serializada — aqui, pelo Handler da main.
 */
class AndroidDeviceScanner : DeviceScanner {
    private val handler = Handler(Looper.getMainLooper())

    override val isAvailable: Boolean = true

    override fun startScan(callback: DeviceScanCallback): Cancellable {
        var found = 0
        lateinit var tick: Runnable
        tick = Runnable {
            found++
            callback.onDeviceFound("android-$found", "Câmera $found")
            if (found < MAX_DEVICES) {
                handler.postDelayed(tick, INTERVAL_MS)
            } else {
                callback.onFinished(null)
            }
        }
        handler.postDelayed(tick, INTERVAL_MS)

        return CallHandle { handler.removeCallbacks(tick) }
    }

    private companion object {
        const val INTERVAL_MS = 800L
        const val MAX_DEVICES = 5
    }
}
