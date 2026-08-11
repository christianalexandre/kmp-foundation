package com.kmpfoundation.di

import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.di.WelcomeGraph
import kotlin.test.Test
import kotlin.test.assertSame

class AppGraphTest {

    @Test
    fun exponeOGrafoDaFeatureRecebido() {
        val welcome = WelcomeGraph(messageRepository = FakeMessageRepository())

        val graph = AppGraph(welcome = welcome)

        assertSame(welcome, graph.welcome)
    }

    @Test
    fun defaultDeProducaoMontaOGrafoSozinho() {
        // É como MainActivity/MainViewController instanciam.
        AppGraph().welcome.viewModel()
    }
}
