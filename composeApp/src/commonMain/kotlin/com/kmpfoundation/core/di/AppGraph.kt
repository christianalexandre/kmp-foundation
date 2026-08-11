package com.kmpfoundation.core.di

import com.kmpfoundation.features.welcome.data.InMemoryMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

/**
 * Composition root manual (sem framework de DI): o grafo inteiro é código
 * explícito. É o único lugar autorizado a conhecer TODAS as features; entre si,
 * features não se importam (regra do style guide). Padrão herdado do
 * di/ImouGraph.kt do imou-smart-home.
 *
 * ViewModels não são criados diretamente: a UI usa `viewModel { }` do
 * lifecycle-compose com as factories daqui, para o ciclo de vida (sobreviver à
 * recriação no Android, escopo por destino na navegação futura) ficar com a
 * plataforma e a montagem do objeto ficar com o grafo.
 *
 * Dependências de plataforma (engines nativas, players) entram como parâmetros
 * do construtor, fornecidas por MainActivity/MainViewController.
 */
class AppGraph(
    private val messageRepository: MessageRepository = InMemoryMessageRepository(),
) {
    fun welcomeViewModel(): WelcomeViewModel =
        WelcomeViewModel(GetWelcomeMessageUseCase(messageRepository))
}
