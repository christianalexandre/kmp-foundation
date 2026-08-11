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

        // O topo apenas compõe: não recria nem embrulha o grafo da feature.
        assertSame(welcome, graph.welcome)
    }

    @Test
    fun defaultDeProducaoMontaOGrafoSozinho() {
        // Garante que o clone consegue instanciar o grafo inteiro sem argumentos
        // — é o que MainActivity/MainViewController fazem.
        AppGraph().welcome.viewModel()
    }
}
