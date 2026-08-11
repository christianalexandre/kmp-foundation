package com.kmpfoundation.di

import com.kmpfoundation.core.platform.FakeGreetingEngine
import kotlin.test.Test
import kotlin.test.assertNotSame

class AppGraphTest {

    @Test
    fun montaOGrafoAPartirDaEngineDePlataforma() {
        val graph = AppGraph(FakeGreetingEngine())

        assertNotSame(graph.welcome.viewModel(), graph.welcome.viewModel())
    }
}
