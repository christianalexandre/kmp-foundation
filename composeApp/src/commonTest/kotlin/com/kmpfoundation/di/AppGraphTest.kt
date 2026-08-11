package com.kmpfoundation.di

import com.kmpfoundation.core.platform.FakeGreetingEngine
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
    fun wiresTheEngineAllTheWayDownToTheScreenState() = runTest(dispatcher) {
        val graph = AppGraph(FakeGreetingEngine(greeting = "hello from the composition root"))

        val viewModel = graph.welcome.viewModel()
        advanceUntilIdle()

        assertEquals(
            WelcomeUiState.Content("hello from the composition root"),
            viewModel.state.value,
        )
    }
}
