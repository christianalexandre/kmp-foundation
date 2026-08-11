package com.kmpfoundation.features.welcome.domain

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/** Caso de uso: uma operação de negócio, invocável como função. */
class GetWelcomeMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(): WelcomeMessage = repository.welcomeMessage()
}
