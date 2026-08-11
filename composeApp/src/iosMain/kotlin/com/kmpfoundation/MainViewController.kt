package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.core.ui.App
import com.kmpfoundation.di.AppGraph
import platform.UIKit.UIViewController

// Recebe o grafo pronto, não a engine: quem é dono do tempo de vida do
// composition root é a casca (`iOSApp.swift`) — ver ADR 0003.
// PascalCase porque o Swift consome isto como se fosse um construtor.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController(graph: AppGraph): UIViewController =
    ComposeUIViewController { App(graph) }
