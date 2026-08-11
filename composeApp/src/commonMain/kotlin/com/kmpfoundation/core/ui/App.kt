package com.kmpfoundation.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.kmpfoundation.core.di.AppGraph
import com.kmpfoundation.features.welcome.ui.WelcomeScreen

/** Raiz Compose compartilhada: tema + (futuramente) casca de navegação. */
@Composable
fun App(graph: AppGraph = remember { AppGraph() }) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val stateHolder = remember(graph) { graph.welcomeStateHolder(scope) }
        WelcomeScreen(stateHolder)
    }
}
