package com.kmpfoundation.features.welcome.domain

interface MessageRepository {
    suspend fun welcomeMessage(): WelcomeMessage
}
