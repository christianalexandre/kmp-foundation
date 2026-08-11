package com.kmpfoundation.features.welcome.domain

import com.kmpfoundation.core.result.Outcome

class GetWelcomeMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(): Outcome<WelcomeMessage> = repository.welcomeMessage()
}
