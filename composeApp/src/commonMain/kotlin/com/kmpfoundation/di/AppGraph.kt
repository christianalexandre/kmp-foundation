package com.kmpfoundation.di

import com.kmpfoundation.features.welcome.di.WelcomeGraph

/**
 * Composition root manual (sem framework de DI): o topo da aplicação, e o único
 * lugar autorizado a conhecer as features. Padrão herdado do `di/ImouGraph.kt`
 * do imou-smart-home; ver ADR 0001 para a decisão e o gatilho de reavaliação.
 *
 * Mora FORA de `core/` de propósito: `core/` é infraestrutura que as features
 * consomem, então um grafo em `core/` criaria dependência circular assim que as
 * features virarem módulos Gradle (feature → core → feature).
 *
 * Cresce **uma linha por feature** — os detalhes de cada uma ficam no grafo
 * dela. Dependências compartilhadas (HttpClient, banco, engines nativas) nascem
 * aqui e são repassadas aos grafos de feature que as pedem.
 */
class AppGraph(
    val welcome: WelcomeGraph = WelcomeGraph(),
)
