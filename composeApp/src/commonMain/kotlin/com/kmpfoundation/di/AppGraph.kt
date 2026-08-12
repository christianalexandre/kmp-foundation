package com.kmpfoundation.di

import com.kmpfoundation.core.platform.DeviceScanner
import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.features.devices.di.DevicesGraph
import com.kmpfoundation.features.welcome.di.WelcomeGraph

/** Composition root — ADR 0001; tempo de vida no ADR 0003. */
class AppGraph(
    greetingEngine: GreetingEngine,
    deviceScanner: DeviceScanner,
) {
    val welcome: WelcomeGraph = WelcomeGraph(greetingEngine)
    val devices: DevicesGraph = DevicesGraph(deviceScanner)
}
