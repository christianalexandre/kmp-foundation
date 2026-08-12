package com.kmpfoundation.features.welcome.di

import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.features.welcome.data.PlatformMessageRepository
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

// Feature de exemplo: apagar a pasta `features/welcome` ao clonar o template.
class WelcomeGraph(
    private val messageRepository: MessageRepository,
) {
    constructor(engine: GreetingEngine) : this(PlatformMessageRepository(engine))

    fun viewModel(): WelcomeViewModel = WelcomeViewModel(messageRepository)
}
