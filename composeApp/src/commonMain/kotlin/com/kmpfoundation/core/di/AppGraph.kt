package com.kmpfoundation.core.di

import com.kmpfoundation.features.welcome.data.InMemoryMessageRepository
import com.kmpfoundation.features.welcome.domain.GetWelcomeMessageUseCase
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.presentation.WelcomeStateHolder
import kotlinx.coroutines.CoroutineScope

/**
 * Composition root manual (sem framework de DI): o grafo inteiro é código
 * explícito. É o único lugar autorizado a conhecer TODAS as features; entre si,
 * features não se importam (regra do style guide). Padrão herdado do
 * di/ImouGraph.kt do imou-smart-home.
 *
 * Dependências de plataforma (engines nativas, players) entram como parâmetros
 * do construtor, fornecidas por MainActivity/MainViewController.
 */
class AppGraph(
    private val messageRepository: MessageRepository = InMemoryMessageRepository(),
) {
    fun welcomeStateHolder(scope: CoroutineScope): WelcomeStateHolder =
        WelcomeStateHolder(GetWelcomeMessageUseCase(messageRepository), scope)
}
