package com.kmpfoundation.features.welcome.domain

import com.kmpfoundation.features.welcome.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetWelcomeMessageUseCaseTest {

    @Test
    fun devolveAMensagemDoRepositorio() = runTest {
        val repository = FakeMessageRepository(WelcomeMessage("mensagem do fake"))
        val useCase = GetWelcomeMessageUseCase(repository)

        val result = useCase()

        assertEquals(WelcomeMessage("mensagem do fake"), result)
        assertEquals(1, repository.callCount)
    }
}
