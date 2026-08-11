package com.kmpfoundation.features.welcome.domain

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/**
 * Porta do domínio: a interface vive aqui e a implementação em `data/`.
 * Nos testes, substitua por um Fake (nunca mock) — ver o style guide.
 */
interface MessageRepository {
    suspend fun welcomeMessage(): WelcomeMessage
}
