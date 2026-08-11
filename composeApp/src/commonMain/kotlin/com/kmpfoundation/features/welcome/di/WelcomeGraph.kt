package com.kmpfoundation.features.welcome.di

import com.kmpfoundation.features.welcome.data.InMemoryMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

// FEATURE-EXEMPLO: demonstra o padrão de camadas do template. Deletar no clone.

/**
 * Grafo da feature: o único lugar que conhece as tripas dela (repositório, use
 * case, ViewModel). O [com.kmpfoundation.di.AppGraph] compõe grafos como este e
 * não enxerga o que há dentro.
 *
 * Dependências compartilhadas (HttpClient, banco, engines nativas) entram por
 * construtor, vindas do topo; os defaults servem produção e testes trocam por
 * Fakes.
 *
 * ViewModel é criado por FACTORY, não guardado aqui: quem controla o ciclo de
 * vida é o `viewModel { }` da UI (ViewModelStore da plataforma).
 */
class WelcomeGraph(
    private val messageRepository: MessageRepository = InMemoryMessageRepository(),
) {
    fun viewModel(): WelcomeViewModel =
        WelcomeViewModel(GetWelcomeMessageUseCase(messageRepository))
}
