package com.kmpfoundation.features.welcome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kmpfoundation.features.welcome.presentation.WelcomeUiState
import com.kmpfoundation.features.welcome.presentation.WelcomeViewModel

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is WelcomeUiState.Loading -> CircularProgressIndicator()
            is WelcomeUiState.Content -> Text(current.message)
        }
    }
}
