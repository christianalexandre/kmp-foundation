package com.kmpfoundation.features.welcome.domain

import com.kmpfoundation.core.result.Outcome

interface MessageRepository {
    suspend fun welcomeMessage(): Outcome<WelcomeMessage>
}
