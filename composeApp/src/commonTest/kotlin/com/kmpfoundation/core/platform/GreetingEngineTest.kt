package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GreetingEngineTest {

    @Test
    fun adapterReturnsWhatTheEngineAnswers() {
        val engine = FakeGreetingEngine(greeting = "hello from fake")

        val result = engine.greet("world")

        assertEquals(Outcome.Success("hello from fake"), result)
        assertEquals("world", engine.lastName)
    }

    @Test
    fun unavailableEngineFailsWithoutCallingPlatform() {
        val engine = FakeGreetingEngine(isAvailable = false)

        val result = engine.greet("world")

        assertEquals(
            Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma")),
            result,
        )
        assertNull(engine.lastName)
    }
}
