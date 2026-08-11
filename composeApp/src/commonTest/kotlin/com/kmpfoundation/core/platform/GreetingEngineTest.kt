package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingEngineTest {

    @Test
    fun adapterReturnsSuccessWhenEngineResponds() = runTest {
        val engine = FakeGreetingEngine(greeting = "hello from fake")

        val result = engine.greet("world")

        assertEquals(Outcome.Success("hello from fake"), result)
        assertEquals("world", engine.lastName)
    }

    @Test
    fun adapterConvertsErrorMessageIntoAppError() = runTest {
        val engine = FakeGreetingEngine(greeting = null, errorMessage = "bluetooth is off")

        val result = engine.greet("world")

        assertEquals(Outcome.Failure(AppError.Platform("bluetooth is off")), result)
    }

    @Test
    fun unavailableEngineFailsWithoutCallingPlatform() = runTest {
        val engine = FakeGreetingEngine(isAvailable = false)

        val result = engine.greet("world")

        assertEquals(
            Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma")),
            result,
        )
        assertEquals(null, engine.lastName)
    }

    @Test
    fun cancellingCoroutineCancelsNativeEngine() = runTest {
        val engine = FakeGreetingEngine(respondImmediately = false)

        val job = launch { engine.greet("world") }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertEquals(1, engine.cancelCount)
    }
}
