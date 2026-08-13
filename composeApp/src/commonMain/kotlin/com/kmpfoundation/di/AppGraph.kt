package com.kmpfoundation.di

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.features.devices.di.DevicesGraph

/**
 * Composition root — ADR 0001; tempo de vida no ADR 0003.
 *
 * Cresce **uma linha por feature**. Com uma raiz Compose por destino (ADR 0007)
 * ele é o único escopo que atravessa todas as telas — o que o torna também o dono
 * do estado compartilhado entre elas.
 */
class AppGraph(
    deviceScanner: DeviceScanner,
) {
    val devices: DevicesGraph = DevicesGraph(deviceScanner)
}
