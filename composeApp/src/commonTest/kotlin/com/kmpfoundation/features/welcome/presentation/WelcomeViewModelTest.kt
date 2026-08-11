package com.kmpfoundation.features.welcome.presentation

import com.kmpfoundation.features.welcome.FakeMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.WelcomeMessage
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

/**
 * O `viewModelScope` usa Dispatchers.Main — nos testes ele é substituído pelo
 * dispatcher de teste via `Dispatchers.setMain`, o padrão oficial para testar
 * ViewModel fora de um device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

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
    fun comecaEmLoading() {
        assertEquals(WelcomeUiState.Loading, viewModel().state.value)
    }

    @Test
    fun loadEmiteContentComAMensagemDoUseCase() = runTest(dispatcher) {
        val viewModel = viewModel(message = "bem-vindo")

        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("bem-vindo"), viewModel.state.value)
    }

    @Test
    fun loadVoltaParaLoadingAntesDeEmitirContent() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.load()

        // Antes de o dispatcher de teste rodar a coroutine, o estado é Loading.
        assertEquals(WelcomeUiState.Loading, viewModel.state.value)
    }

    private fun viewModel(message: String = "olá"): WelcomeViewModel =
        WelcomeViewModel(GetWelcomeMessageUseCase(FakeMessageRepository(WelcomeMessage(message))))
}
