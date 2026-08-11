package com.kmpfoundation.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.features.welcome.ui.WelcomeScreen

/** Raiz Compose compartilhada: tema + (futuramente) casca de navegação. */
@Composable
fun App(graph: AppGraph = remember { AppGraph() }) {
    MaterialTheme {
        // `viewModel {}` guarda a instância no ViewModelStore da plataforma
        // (sobrevive à recriação da Activity); o grafo da feature só a monta.
        val welcomeViewModel = viewModel { graph.welcome.viewModel() }
        WelcomeScreen(welcomeViewModel)
    }
}
