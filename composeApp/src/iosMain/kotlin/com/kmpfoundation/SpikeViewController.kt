package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.ui.spike.SpikeScreen
import platform.UIKit.UIViewController

/**
 * Uma raiz Compose **por destino** — a forma que a opção 3 do RFC 0001 exige, e o
 * objeto exato que o spike mede. Cada chamada devolve um
 * `ComposeUIViewController` novo, com composição própria, para entrar num
 * `UINavigationController` real.
 *
 * Contraste com [MainViewController], que é raiz **única** para o app inteiro —
 * é a diferença entre a opção 2 e a opção 3 do RFC, em uma linha de código.
 *
 * PascalCase porque o Swift consome isto como se fosse um construtor.
 */
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun SpikeViewController(
    depth: Int,
    onFirstDraw: () -> Unit,
    onPushNext: () -> Unit,
    onPushNative: () -> Unit,
): UIViewController = ComposeUIViewController {
    SpikeScreen(
        depth = depth,
        onFirstDraw = onFirstDraw,
        onPushNext = onPushNext,
        onPushNative = onPushNative,
    )
}
