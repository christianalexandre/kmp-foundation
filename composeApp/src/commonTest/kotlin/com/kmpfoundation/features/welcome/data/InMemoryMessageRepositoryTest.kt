package com.kmpfoundation.features.welcome.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class InMemoryMessageRepositoryTest {

    @Test
    fun devolveMensagemNaoVazia() = runTest {
        val repository = InMemoryMessageRepository()

        val message = repository.welcomeMessage()

        assertTrue(message.text.isNotBlank())
    }
}
