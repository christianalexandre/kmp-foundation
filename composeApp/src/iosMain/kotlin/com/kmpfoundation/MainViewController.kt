package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.core.ui.App
import platform.UIKit.UIViewController

/**
 * Entry point consumido pelo iosApp (Swift). Dependências de plataforma
 * (engines nativas etc.) entram como parâmetros desta função e seguem para o
 * AppGraph — padrão do imou-smart-home.
 */
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
