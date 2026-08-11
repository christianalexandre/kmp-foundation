package com.kmpfoundation.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.features.welcome.ui.WelcomeScreen

@Composable
fun App(graph: AppGraph = remember { AppGraph() }) {
    MaterialTheme {
        val welcomeViewModel = viewModel { graph.welcome.viewModel() }
        WelcomeScreen(welcomeViewModel)
    }
}
