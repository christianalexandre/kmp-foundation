package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.core.platform.greet
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.core.result.map
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

/**
 * Busca a mensagem na engine nativa. Repare que a camada de dados não conhece
 * plataforma: fala com a interface, e quem implementa é Kotlin no Android e
 * Swift no iOS.
 */
class PlatformMessageRepository(
    private val engine: GreetingEngine,
) : MessageRepository {
    override suspend fun welcomeMessage(): Outcome<WelcomeMessage> =
        engine.greet(APP_NAME).map(::WelcomeMessage)

    private companion object {
        const val APP_NAME = "kmp-foundation"
    }
}
