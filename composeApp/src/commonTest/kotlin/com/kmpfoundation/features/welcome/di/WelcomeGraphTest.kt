package com.kmpfoundation.features.welcome.di

import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeGraphTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun montaOViewModelComAsDependenciasRecebidas() = runTest(dispatcher) {
        val repository = FakeMessageRepository()
        val graph = WelcomeGraph(messageRepository = repository)

        val viewModel = graph.viewModel()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("olá dos testes"), viewModel.state.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun viewModelEhFactory_naoInstanciaCompartilhada() {
        val graph = WelcomeGraph(messageRepository = FakeMessageRepository())

        // O grafo NÃO guarda o ViewModel: quem controla o ciclo de vida é o
        // `viewModel { }` da UI. Cada chamada devolve uma instância nova.
        assertNotSame(graph.viewModel(), graph.viewModel())
    }
}
