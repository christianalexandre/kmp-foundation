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
final class RootCoordinator: NSObject, UINavigationControllerDelegate {

    private let graph: AppGraph
    private weak var navigationController: UINavigationController?

    init(graph: AppGraph) {
        self.graph = graph
    }

    func makeNavigationController() -> UINavigationController {
        let root = controller(for: NavigationPolicy.shared.start)
        let nav = UINavigationController(rootViewController: root)
        nav.delegate = self
        navigationController = nav
        return nav
    }

    /// Entrada de deeplink: empilha a **pilha inteira** que a política devolveu,
    /// não só a última tela — senão o voltar joga o usuário para fora do app.
    func open(deeplink: String) {
        guard let nav = navigationController,
              let routes = NavigationPolicy.shared.fromDeeplink(uri: deeplink) else { return }

        let controllers = routes.map { controller(for: $0) }
        nav.setViewControllers(controllers, animated: true)
    }

    // MARK: - Apresentação

    private func navigate(to route: Route) {
        navigationController?.pushViewController(controller(for: route), animated: true)
    }

    /// O único ponto do app que sabe **como** cada tipo de destino é montado.
    ///
    /// Os nomes são achatados (`RouteDeviceDetail`, não `Route.DeviceDetail`): uma
    /// `sealed interface` do Kotlin vira **protocolo** em Objective-C, e protocolo
    /// não tem tipo aninhado. É a mesma restrição de exportação que o SDD 02 já
    /// registra para os contratos de engine.
    private func controller(for route: Route) -> UIViewController {
        let controller: UIViewController

        if let detail = route as? RouteDeviceDetail {
            // Destino nativo: SwiftUI puro, sem Compose. É onde uma tela de SDK
            // entraria no app real.
            controller = UIHostingController(
                rootView: DeviceDetailView(
                    deviceId: detail.deviceId,
                    onOpenSettings: { [weak self] in
                        self?.navigate(to: NavigationPolicy.shared.onDeviceSettings(deviceId: detail.deviceId))
                    }
                )
            )
        } else {
            controller = RouteViewControllerKt.RouteViewController(
                route: route,
                graph: graph,
                onNavigate: { [weak self] next in
                    self?.navigate(to: next)
                }
            )
        }

        controller.title = title(for: route)
        // Sem isto o conteúdo se estende por baixo da nav bar e ela cobre o topo da
        // tela. A nav bar precisa estar em cena — é ela que dá o botão de voltar e a
        // transição de push — então quem cede é o conteúdo.
        controller.edgesForExtendedLayout = []
        return controller
    }

    private func title(for route: Route) -> String {
        switch route {
        case is RouteWelcome: return "Início"
        case is RouteDevices: return "Dispositivos"
        case let detail as RouteDeviceDetail: return detail.deviceId
        case is RouteDeviceSettings: return "Ajustes"
        default: return ""
        }
    }

    // MARK: - UINavigationControllerDelegate

    func navigationController(
        _ navigationController: UINavigationController,
        willShow viewController: UIViewController,
        animated: Bool
    ) {
        // A raiz desenha o próprio cabeçalho; as telas empilhadas usam a nav bar
        // real, que é quem dá o botão de voltar e o crossfade do título.
        let isRoot = viewController === navigationController.viewControllers.first
        navigationController.setNavigationBarHidden(isRoot, animated: animated)
    }
}
