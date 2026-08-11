package com.kmpfoundation.features.welcome

import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

/**
 * Fake, não mock (ver style guide): implementação real e mínima da porta,
 * com estado inspecionável pelo teste.
 */
class FakeMessageRepository(
    private val message: WelcomeMessage = WelcomeMessage("olá dos testes"),
) : MessageRepository {
    var callCount = 0
        private set

    override suspend fun welcomeMessage(): WelcomeMessage {
        callCount++
        return message
    }
}
