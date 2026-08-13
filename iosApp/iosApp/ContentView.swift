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
        RootView(graph: graph)
            .ignoresSafeArea(.keyboard)
    }
}
