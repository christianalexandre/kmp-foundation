package com.kmpfoundation.features.welcome.presentation

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.FakeMessageRepository
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

// `viewModelScope` runs on Dispatchers.Main, absent outside a device.
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
    fun startsInLoadingState() {
        assertEquals(WelcomeUiState.Loading, viewModel().state.value)
    }

    @Test
    fun loadsOnCreationWithoutTheScreenAskingForIt() = runTest(dispatcher) {
        val repository = FakeMessageRepository(Outcome.Success(WelcomeMessage("welcome")))

        val viewModel = WelcomeViewModel(repository)
        advanceUntilIdle()

        assertEquals(WelcomeUiState.Content("welcome"), viewModel.state.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun retryWhileALoadIsInFlightIsIgnored() = runTest(dispatcher) {
        val repository = FakeMessageRepository()
        val viewModel = WelcomeViewModel(repository)

        // The load started by `init` has not run yet on the test dispatcher.
        viewModel.load()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    @Test
    fun retryAfterAFinishedLoadRunsAgain() = runTest(dispatcher) {
        val repository = FakeMessageRepository()
        val viewModel = WelcomeViewModel(repository)
        advanceUntilIdle()

        viewModel.load()
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }

    @Test
    fun connectivityFailureOffersRetry() = runTest(dispatcher) {
        val viewModel = viewModel(Outcome.Failure(AppError.Connectivity))

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is WelcomeUiState.Error && state.canRetry)
    }

    @Test
    fun platformFailureDoesNotOfferRetry() = runTest(dispatcher) {
        val viewModel = viewModel(Outcome.Failure(AppError.Platform("no bluetooth")))

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is WelcomeUiState.Error && !state.canRetry)
    }

    @Test
    fun eachDomainErrorHasItsOwnUiMessage() = runTest(dispatcher) {
        val errors = listOf(
            AppError.Connectivity,
            AppError.NotFound,
            AppError.Unauthorized,
            AppError.Platform("x"),
            AppError.Unexpected(),
        )

        val messages = errors.map { error ->
            val viewModel = viewModel(Outcome.Failure(error))
            advanceUntilIdle()
            (viewModel.state.value as WelcomeUiState.Error).message
        }

        assertEquals(messages.size, messages.distinct().size, "duplicate messages: $messages")
    }

    private fun viewModel(
        outcome: Outcome<WelcomeMessage> = Outcome.Success(WelcomeMessage("hello")),
    ): WelcomeViewModel =
        WelcomeViewModel(FakeMessageRepository(outcome))
}
