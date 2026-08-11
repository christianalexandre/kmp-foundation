package com.kmpfoundation.features.welcome

import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

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
