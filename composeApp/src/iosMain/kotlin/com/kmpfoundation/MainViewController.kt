package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.core.ui.App
import platform.UIKit.UIViewController

// PascalCase porque o Swift consome isto como se fosse um construtor.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
