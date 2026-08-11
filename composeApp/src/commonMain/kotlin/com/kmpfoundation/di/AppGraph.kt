package com.kmpfoundation.di

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.features.welcome.di.WelcomeGraph

/**
 * Composition root — ver ADR 0001. Cresce uma linha por feature.
 *
 * As engines nativas chegam prontas de `MainActivity`/`MainViewController`,
 * porque só a casca de cada plataforma sabe construí-las.
 */
class AppGraph(engine: GreetingEngine) {
    val welcome: WelcomeGraph = WelcomeGraph(engine)
}
