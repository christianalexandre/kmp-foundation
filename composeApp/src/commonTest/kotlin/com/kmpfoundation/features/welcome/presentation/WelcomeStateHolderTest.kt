package com.kmpfoundation.features.welcome.presentation

import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.WelcomeMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeStateHolderTest {

    @Test
    fun comecaEmLoading() = runTest {
        val holder = holder(this)

        assertEquals(WelcomeUiState.Loading, holder.state.value)
    }

    @Test
    fun loadEmiteContentComAMensagemDoUseCase() = runTest {
        val holder = holder(this, message = "bem-vindo")

        holder.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("bem-vindo"), holder.state.value)
    }

    @Test
    fun loadVoltaParaLoadingAntesDeEmitirContent() = runTest {
        val holder = holder(this)

        holder.load()

        // Antes de o dispatcher de teste rodar a coroutine, o estado é Loading.
        assertEquals(WelcomeUiState.Loading, holder.state.value)
    }

    private fun holder(
        scope: kotlinx.coroutines.CoroutineScope,
        message: String = "olá",
    ): WelcomeStateHolder {
        val useCase = GetWelcomeMessageUseCase(FakeMessageRepository(WelcomeMessage(message)))
        return WelcomeStateHolder(useCase, scope)
    }
}
