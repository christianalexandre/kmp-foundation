package com.kmpfoundation

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpfoundation.di.AppGraph
import com.kmpfoundation.nav.Route
import com.kmpfoundation.ui.RouteContent
import platform.UIKit.UIViewController

/**
 * Uma raiz Compose **por destino** — a forma que o ADR 0007 decidiu.
 *
 * Cada chamada devolve um `ComposeUIViewController` novo, com composição própria,
 * para entrar no `UINavigationController` da casca. Medido: cada um traz seu
 * próprio `ViewModelStoreOwner`, então o escopo de ViewModel por tela vem do
 * sistema — é o que dispensa uma biblioteca de navegação aqui.
 *
 * PascalCase porque o Swift consome isto como se fosse um construtor.
 *
 * @param onBack `null` no destino que é raiz da pilha — ver `RouteContent`.
 */
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun RouteViewController(
    route: Route,
    graph: AppGraph,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)?,
): UIViewController = ComposeUIViewController {
    RouteContent(route = route, graph = graph, onNavigate = onNavigate, onBack = onBack)
}
