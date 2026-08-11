package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

class InMemoryMessageRepository : MessageRepository {
    override suspend fun welcomeMessage(): WelcomeMessage =
        WelcomeMessage("Bem-vindo ao kmp-foundation")
}
