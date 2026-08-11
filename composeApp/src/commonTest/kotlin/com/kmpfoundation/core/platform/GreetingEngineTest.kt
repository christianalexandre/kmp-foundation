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
    fun adapterDevolveSucessoQuandoAEngineResponde() = runTest {
        val engine = FakeGreetingEngine(greeting = "olá do fake")

        val result = engine.greet("mundo")

        assertEquals(Outcome.Success("olá do fake"), result)
        assertEquals("mundo", engine.lastName)
    }

    @Test
    fun adapterConverteMensagemDeErroEmAppError() = runTest {
        val engine = FakeGreetingEngine(greeting = null, errorMessage = "bluetooth desligado")

        val result = engine.greet("mundo")

        assertEquals(Outcome.Failure(AppError.Platform("bluetooth desligado")), result)
    }

    @Test
    fun engineIndisponivelFalhaSemChamarAPlataforma() = runTest {
        val engine = FakeGreetingEngine(isAvailable = false)

        val result = engine.greet("mundo")

        assertEquals(
            Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma")),
            result,
        )
        assertEquals(null, engine.lastName)
    }

    @Test
    fun cancelarACoroutineCancelaAEngineNativa() = runTest {
        val engine = FakeGreetingEngine(respondImmediately = false)

        val job = launch { engine.greet("mundo") }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertEquals(1, engine.cancelCount)
    }
}
