package com.kmpfoundation.features.welcome.data

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.core.platform.greet
import com.kmpfoundation.core.platform.platformName
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.core.result.map
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

class PlatformMessageRepository(
    private val engine: GreetingEngine,
) : MessageRepository {
    // O texto é composto aqui, e não dentro de cada engine: senão a mesma
    // string de produto existiria duas vezes, em Kotlin e em Swift.
    override suspend fun welcomeMessage(): Outcome<WelcomeMessage> =
        engine.greet(APP_NAME).map { WelcomeMessage("$it — de ${platformName()}") }

    private companion object {
        const val APP_NAME = "kmp-foundation"
    }
}
