package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.core.platform.FakeGreetingEngine
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.domain.WelcomeMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformMessageRepositoryTest {

    @Test
    fun embrulhaARespostaDaEngineNaEntidadeDeDominio() = runTest {
        val engine = FakeGreetingEngine(greeting = "Olá, kmp-foundation")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(Outcome.Success(WelcomeMessage("Olá, kmp-foundation")), result)
    }

    @Test
    fun propagaFalhaDaPlataforma() = runTest {
        val engine = FakeGreetingEngine(greeting = null, errorMessage = "sem permissão")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(Outcome.Failure(AppError.Platform("sem permissão")), result)
    }
}
