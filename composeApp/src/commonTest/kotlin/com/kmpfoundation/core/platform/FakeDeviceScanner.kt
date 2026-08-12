package com.kmpfoundation.core.platform

/**
 * Fake da busca nativa. Nada acontece sozinho: o teste dirige o tempo chamando
 * [emit] e [finish], que é o que permite verificar o cancelamento sem device.
 */
class FakeDeviceScanner(
    override val isAvailable: Boolean = true,
) : DeviceScanner {
    var startCount = 0
        private set
    var cancelCount = 0
        private set

    private var callback: DeviceScanCallback? = null

    val isScanning: Boolean get() = callback != null

    override fun startScan(callback: DeviceScanCallback): Cancellable {
        startCount++
        this.callback = callback
        return CallHandle {
            cancelCount++
            this.callback = null
        }
    }

    fun emit(id: String, name: String) {
        callback?.onDeviceFound(id, name)
    }

    fun finish(errorMessage: String? = null) {
        callback?.onFinished(errorMessage)
        callback = null
    }
}
