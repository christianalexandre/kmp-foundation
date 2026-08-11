package com.kmpfoundation.features.welcome.di

import com.kmpfoundation.core.platform.FakeGreetingEngine
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage
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
        val repository = FakeMessageRepository(Outcome.Success(WelcomeMessage("olá dos testes")))
        val graph = WelcomeGraph(messageRepository = repository)

        val viewModel = graph.viewModel()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("olá dos testes"), viewModel.state.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun montaAPartirDaEngineDePlataforma() = runTest(dispatcher) {
        val graph = WelcomeGraph(FakeGreetingEngine(greeting = "olá do fake"))

        val viewModel = graph.viewModel()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("olá do fake"), viewModel.state.value)
    }

    @Test
    fun viewModelEhFactory() {
        val graph = WelcomeGraph(FakeGreetingEngine())

        assertNotSame(graph.viewModel(), graph.viewModel())
    }
}
