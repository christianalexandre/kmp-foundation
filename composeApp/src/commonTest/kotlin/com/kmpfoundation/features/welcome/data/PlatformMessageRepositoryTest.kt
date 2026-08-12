package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.core.platform.FakeGreetingEngine
import com.kmpfoundation.core.platform.platformName
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.domain.WelcomeMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformMessageRepositoryTest {

    @Test
    fun composesTheEngineGreetingWithThePlatformName() = runTest {
        val engine = FakeGreetingEngine(greeting = "Olá, kmp-foundation")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(
            Outcome.Success(WelcomeMessage("Olá, kmp-foundation — de ${platformName()}")),
            result,
        )
    }

    @Test
    fun asksTheEngineForTheAppName() = runTest {
        val engine = FakeGreetingEngine()

        PlatformMessageRepository(engine).welcomeMessage()

        assertEquals("kmp-foundation", engine.calls.single().name)
    }

    @Test
    fun propagatesPlatformFailure() = runTest {
        val engine = FakeGreetingEngine(greeting = null, errorMessage = "permission denied")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(Outcome.Failure(AppError.Platform("permission denied")), result)
    }
}
