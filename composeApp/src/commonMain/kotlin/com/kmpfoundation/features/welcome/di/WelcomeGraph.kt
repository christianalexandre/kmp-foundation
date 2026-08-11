package com.kmpfoundation.features.welcome.di

import com.kmpfoundation.features.welcome.data.InMemoryMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

// Feature de exemplo: apagar a pasta `features/welcome` ao clonar o template.
class WelcomeGraph(
    private val messageRepository: MessageRepository = InMemoryMessageRepository(),
) {
    // Factory, não instância: o ciclo de vida é do `viewModel { }` da UI.
    fun viewModel(): WelcomeViewModel =
        WelcomeViewModel(GetWelcomeMessageUseCase(messageRepository))
}
