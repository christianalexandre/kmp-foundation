package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingEngineTest {

    @Test
    fun adapterReturnsSuccessWhenEngineResponds() = runTest {
        val engine = FakeGreetingEngine(greeting = "hello from fake")

        val result = engine.greet("world")

        assertEquals(Outcome.Success("hello from fake"), result)
        assertEquals("world", engine.calls.single().name)
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
        assertTrue(engine.calls.isEmpty())
    }

    @Test
    fun cancellingCoroutineCancelsNativeCall() = runTest {
        val engine = FakeGreetingEngine(respondImmediately = false)

        val job = launch { engine.greet("world") }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertTrue(engine.calls.single().isCancelled)
    }

    @Test
    fun cancellingOneCallLeavesConcurrentCallsRunning() = runTest {
        val engine = FakeGreetingEngine(respondImmediately = false)
        var secondResult: Outcome<String>? = null

        val first = launch { engine.greet("first") }
        launch { secondResult = engine.greet("second") }
        advanceUntilIdle()
        first.cancel()
        advanceUntilIdle()

        val (firstCall, secondCall) = engine.calls
        assertTrue(firstCall.isCancelled, "the cancelled coroutine must cancel its own call")
        assertFalse(secondCall.isCancelled, "a sibling call must survive")

        // And it is not merely uncancelled: it still resolves.
        engine.respondTo(secondCall, greeting = "still alive")
        advanceUntilIdle()
        assertEquals(Outcome.Success("still alive"), secondResult)
    }

    @Test
    fun concurrentCallsResolveIndependently() = runTest {
        val engine = FakeGreetingEngine(respondImmediately = false)
        var firstResult: Outcome<String>? = null
        var secondResult: Outcome<String>? = null

        launch { firstResult = engine.greet("first") }
        launch { secondResult = engine.greet("second") }
        advanceUntilIdle()
        // Out of order on purpose: a native SDK answers when it answers.
        engine.respondTo(engine.calls[1], greeting = "answer for second")
        engine.respondTo(engine.calls[0], greeting = "answer for first")
        advanceUntilIdle()

        assertEquals(Outcome.Success("answer for first"), firstResult)
        assertEquals(Outcome.Success("answer for second"), secondResult)
    }
}
