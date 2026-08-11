package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/**
 * Implementação da porta do domínio. Num app real, aqui entram as fontes de
 * dados de verdade (HTTP, banco, SDK nativo via expect/actual).
 */
class InMemoryMessageRepository : MessageRepository {
    override suspend fun welcomeMessage(): WelcomeMessage =
        WelcomeMessage("Bem-vindo ao kmp-foundation")
}
