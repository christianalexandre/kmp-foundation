package com.kmpfoundation.core.result

import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {

    @Test
    fun mapTransformaOValorDoSucesso() {
        val result = Outcome.Success(2).map { it * 3 }

        assertEquals(Outcome.Success(6), result)
    }

    @Test
    fun mapPreservaAFalhaSemExecutarATransformacao() {
        val failure: Outcome<Int> = Outcome.Failure(AppError.Connectivity)

        val result = failure.map { error("não deveria ser chamado") }

        assertEquals(Outcome.Failure(AppError.Connectivity), result)
    }
}
