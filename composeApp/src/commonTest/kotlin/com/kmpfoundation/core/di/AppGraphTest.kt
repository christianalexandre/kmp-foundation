package com.kmpfoundation.core.di

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

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

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
    fun montaOGrafoComDependenciasSubstituiveis() = runTest(dispatcher) {
        val repository = FakeMessageRepository()
        val graph = AppGraph(messageRepository = repository)

        val viewModel = graph.welcomeViewModel()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("olá dos testes"), viewModel.state.value)
        assertEquals(1, repository.callCount)
    }
}
