package com.kmpfoundation.features.welcome.presentation

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
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
import kotlin.test.assertTrue

// `viewModelScope` roda em Dispatchers.Main, que não existe fora de um device.
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
        val viewModel = viewModel(Outcome.Success(WelcomeMessage("bem-vindo")))

        viewModel.load()
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("bem-vindo"), viewModel.state.value)
    }

    @Test
    fun falhaDeConectividadeOfereceTentarDeNovo() = runTest(dispatcher) {
        val viewModel = viewModel(Outcome.Failure(AppError.Connectivity))

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is WelcomeUiState.Error && state.canRetry)
    }

    @Test
    fun falhaDePlataformaNaoOfereceTentarDeNovo() = runTest(dispatcher) {
        val viewModel = viewModel(Outcome.Failure(AppError.Platform("sem bluetooth")))

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is WelcomeUiState.Error && !state.canRetry)
    }

    @Test
    fun cadaErroDeDominioTemUmaMensagemDeUiPropria() = runTest(dispatcher) {
        val errors = listOf(
            AppError.Connectivity,
            AppError.NotFound,
            AppError.Unauthorized,
            AppError.Platform("x"),
            AppError.Unexpected(),
        )

        val messages = errors.map { error ->
            val viewModel = viewModel(Outcome.Failure(error))
            viewModel.load()
            advanceUntilIdle()
            (viewModel.state.value as WelcomeUiState.Error).message
        }

        assertEquals(messages.size, messages.distinct().size, "mensagens repetidas: $messages")
    }

    private fun viewModel(
        outcome: Outcome<WelcomeMessage> = Outcome.Success(WelcomeMessage("olá")),
    ): WelcomeViewModel =
        WelcomeViewModel(GetWelcomeMessageUseCase(FakeMessageRepository(outcome)))
}
