package com.kmpfoundation.features.welcome.domain

class GetWelcomeMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(): WelcomeMessage = repository.welcomeMessage()
}
