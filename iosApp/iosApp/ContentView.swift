import ComposeApp
import SwiftUI
import UIKit

/// Hospeda a casca de navegação — ver RootCoordinator.
struct RootView: UIViewControllerRepresentable {

    let graph: AppGraph

    func makeCoordinator() -> RootCoordinator { RootCoordinator(graph: graph) }

    func makeUIViewController(context: Context) -> UINavigationController {
        context.coordinator.makeNavigationController()
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {}
}

struct ContentView: View {
    let graph: AppGraph

    var body: some View {
        // De borda a borda de propósito: com a nav bar escondida, quem cede espaço
        // para as barras do sistema é o conteúdo de cada destino — o Compose por
        // `safeDrawingPadding`, o SwiftUI pela safe area dele. Insetar aqui
        // insetaria duas vezes.
        RootView(graph: graph)
            .ignoresSafeArea()
    }
}
