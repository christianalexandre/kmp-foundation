package com.kmpfoundation.di

import com.kmpfoundation.features.welcome.di.WelcomeGraph

/** Composition root — ver ADR 0001. Cresce uma linha por feature. */
class AppGraph(
    val welcome: WelcomeGraph = WelcomeGraph(),
)
