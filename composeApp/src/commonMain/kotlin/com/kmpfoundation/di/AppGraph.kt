package com.kmpfoundation.di

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.features.welcome.di.WelcomeGraph

/**
 * Composition root — ver ADR 0001. Cresce uma linha por feature.
 *
 * As engines nativas chegam prontas da casca de cada plataforma, que é quem
 * sabe construí-las. **Quem instancia este grafo é o dono do ciclo de vida da
 * aplicação** (`KmpFoundationApplication` / `iOSApp`), nunca uma tela — ADR 0003.
 */
class AppGraph(engine: GreetingEngine) {
    val welcome: WelcomeGraph = WelcomeGraph(engine)
}
