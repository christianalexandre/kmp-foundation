package com.kmpfoundation.core.di

import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

    @Test
    fun montaOGrafoComDependenciasSubstituiveis() = runTest {
        val repository = FakeMessageRepository()
        val graph = AppGraph(messageRepository = repository)

        val holder = graph.welcomeStateHolder(this)
        holder.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("olá dos testes"), holder.state.value)
        assertEquals(1, repository.callCount)
    }
}
