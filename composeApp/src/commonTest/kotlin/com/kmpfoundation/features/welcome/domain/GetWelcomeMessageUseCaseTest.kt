package com.kmpfoundation.features.welcome.domain

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetWelcomeMessageUseCaseTest {

    @Test
    fun devolveAMensagemDoRepositorio() = runTest {
        val repository = FakeMessageRepository(Outcome.Success(WelcomeMessage("mensagem do fake")))

        val result = GetWelcomeMessageUseCase(repository)()

        assertEquals(Outcome.Success(WelcomeMessage("mensagem do fake")), result)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun propagaAFalhaSemTraduzir() = runTest {
        val repository = FakeMessageRepository.failing(AppError.Connectivity)

        val result = GetWelcomeMessageUseCase(repository)()

        assertEquals(Outcome.Failure(AppError.Connectivity), result)
    }
}
