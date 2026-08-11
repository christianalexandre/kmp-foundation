package com.kmpfoundation.features.welcome.domain

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetWelcomeMessageUseCaseTest {

    @Test
    fun returnsMessageFromRepository() = runTest {
        val repository = FakeMessageRepository(Outcome.Success(WelcomeMessage("message from fake")))

        val result = GetWelcomeMessageUseCase(repository)()

        assertEquals(Outcome.Success(WelcomeMessage("message from fake")), result)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun propagatesFailureWithoutTranslating() = runTest {
        val repository = FakeMessageRepository.failing(AppError.Connectivity)

        val result = GetWelcomeMessageUseCase(repository)()

        assertEquals(Outcome.Failure(AppError.Connectivity), result)
    }
}
