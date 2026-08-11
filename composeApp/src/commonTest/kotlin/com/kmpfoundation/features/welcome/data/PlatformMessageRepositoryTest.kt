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
    fun wrapsEngineResponseInDomainEntity() = runTest {
        val engine = FakeGreetingEngine(greeting = "Hello, kmp-foundation")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(Outcome.Success(WelcomeMessage("Hello, kmp-foundation")), result)
    }

    @Test
    fun propagatesPlatformFailure() = runTest {
        val engine = FakeGreetingEngine(greeting = null, errorMessage = "permission denied")

        val result = PlatformMessageRepository(engine).welcomeMessage()

        assertEquals(Outcome.Failure(AppError.Platform("permission denied")), result)
    }
}
