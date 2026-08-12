package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.ui.App
import platform.UIKit.UIViewController

// PascalCase porque o Swift consome isto como se fosse um construtor.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController(graph: AppGraph): UIViewController =
    ComposeUIViewController { App(graph) }
