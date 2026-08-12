package com.kmpfoundation.di

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.features.welcome.di.WelcomeGraph

/** Composition root — ADR 0001; tempo de vida no ADR 0003. */
class AppGraph(engine: GreetingEngine) {
    val welcome: WelcomeGraph = WelcomeGraph(engine)
}
