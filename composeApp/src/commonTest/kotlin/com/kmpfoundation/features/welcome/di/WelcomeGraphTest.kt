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
    fun buildsViewModelWithProvidedDependencies() = runTest(dispatcher) {
        val repository = FakeMessageRepository(Outcome.Success(WelcomeMessage("hello from tests")))
        val graph = WelcomeGraph(messageRepository = repository)

        val viewModel = graph.viewModel()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("hello from tests"), viewModel.state.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun buildsFromPlatformEngine() = runTest(dispatcher) {
        val graph = WelcomeGraph(FakeGreetingEngine(greeting = "hello from fake"))

        val viewModel = graph.viewModel()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("hello from fake"), viewModel.state.value)
    }

    @Test
    fun viewModelIsAFactory() {
        val graph = WelcomeGraph(FakeGreetingEngine())

        assertNotSame(graph.viewModel(), graph.viewModel())
    }
}
