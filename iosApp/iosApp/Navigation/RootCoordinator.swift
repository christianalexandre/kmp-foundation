import ComposeApp
import SwiftUI
import UIKit

/// Casca de navegação do iOS — ADR 0007.
///
/// O `UINavigationController` é dono da pilha, e cada destino é seu próprio view
/// controller: um `ComposeUIViewController` para as telas Compose, um
/// `UIHostingController` para as nativas. É isso que dá transição de push e
/// swipe-back do UIKit de verdade, inclusive entre telas Compose.
///
/// **Esta casca não decide destino.** Ela recebe uma `Route` já escolhida pela
/// `NavigationPolicy` e sabe apenas como apresentá-la. Não existe pilha em Kotlin
/// espelhando esta — sem espelho, não há sincronização a manter nem laço a evitar.
///
/// ## A nav bar fica escondida SEMPRE
///
/// Decisão de produto: a tela inicial não tem cabeçalho, e as demais têm — nas
/// duas plataformas, com o mesmo desenho. Isso descarta as duas formas de usar a
/// barra do UIKit:
///
/// - **Esconder só na raiz** é um bug já pago: ao empilhar, a barra anima para
///   dentro enquanto o conteúdo novo já começa abaixo dela, e a faixa da barra
///   mostra a tela de saída por alguns frames.
/// - **Sempre visível com título vazio na raiz** deixa uma faixa em branco no topo
///   da tela inicial, que é chrome visível.
///
/// Então o cabeçalho é desenhado em Compose (`ui/RouteContent.kt`) e em SwiftUI
/// nos destinos nativos, com o texto vindo de `titleOf` — um lugar só, comum às
/// duas plataformas. O preço é este arquivo: sem a barra, o UIKit desliga o gesto
/// de voltar, e ele precisa ser reabilitado à mão pelo delegate abaixo.
final class RootCoordinator: NSObject {

    private let graph: AppGraph
    private weak var navigationController: UINavigationController?

    init(graph: AppGraph) {
        self.graph = graph
    }

    func makeNavigationController() -> UINavigationController {
        let root = controller(for: NavigationPolicy.shared.start, canGoBack: false)
        let nav = UINavigationController(rootViewController: root)
        nav.setNavigationBarHidden(true, animated: false)
        // Sem a barra, o UIKit desabilita o swipe-back. Assumir o delegate o traz de
        // volta — é o gesto do sistema, não uma imitação, que é o motivo do ADR 0007.
        nav.interactivePopGestureRecognizer?.delegate = self
        navigationController = nav
        return nav
    }

    // MARK: - Apresentação

    private func navigate(to route: Route) {
        let next = controller(for: route, canGoBack: true)
        navigationController?.pushViewController(next, animated: true)
    }

    private func pop() {
        navigationController?.popViewController(animated: true)
    }

    /// O único ponto do app que sabe **como** cada tipo de destino é montado.
    ///
    /// Os nomes são achatados (`RouteDeviceDetail`, não `Route.DeviceDetail`): uma
    /// `sealed interface` do Kotlin vira **protocolo** em Objective-C, e protocolo
    /// não tem tipo aninhado. É a mesma restrição de exportação que o SDD 02 já
    /// registra para os contratos de engine.
    ///
    /// `canGoBack` é `false` só para a raiz da pilha, e é o que decide se existe
    /// cabeçalho. Quem sabe disso é esta casca, porque a pilha é dela.
    private func controller(for route: Route, canGoBack: Bool) -> UIViewController {
        let onBack: (() -> Void)? = canGoBack ? { [weak self] in self?.pop() } : nil

        if let detail = route as? RouteDeviceDetail {
            // Destino nativo: SwiftUI puro, sem Compose. É onde uma tela de SDK
            // entraria no app real.
            return UIHostingController(
                rootView: DeviceDetailView(
                    deviceId: detail.deviceId,
                    title: RouteTitleKt.titleOf(route: route),
                    onOpenSettings: { [weak self] in
                        self?.navigate(to: NavigationPolicy.shared.onDeviceSettings(deviceId: detail.deviceId))
                    },
                    onBack: onBack
                )
            )
        }

        return RouteViewControllerKt.RouteViewController(
            route: route,
            graph: graph,
            onNavigate: { [weak self] next in
                self?.navigate(to: next)
            },
            onBack: onBack
        )
    }

}

extension RootCoordinator: UIGestureRecognizerDelegate {

    /// Sem isto o gesto dispararia também na raiz, onde não há para onde voltar —
    /// o UIKit trava a navegação quando isso acontece.
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        (navigationController?.viewControllers.count ?? 0) > 1
    }
}
