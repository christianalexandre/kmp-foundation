package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.core.platform.GreetingEngine
import com.kmpfoundation.core.ui.App
import com.kmpfoundation.di.AppGraph
import platform.UIKit.UIViewController

// PascalCase porque o Swift consome isto como se fosse um construtor.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController(greetingEngine: GreetingEngine): UIViewController =
    ComposeUIViewController { App(AppGraph(greetingEngine)) }
