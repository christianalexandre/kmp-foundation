import SwiftUI
import UIKit
import ComposeApp

/// Hospeda a raiz Compose exportada no framework ComposeApp.
struct ComposeView: UIViewControllerRepresentable {
    let graph: AppGraph

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(graph: graph)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let graph: AppGraph

    var body: some View {
        ComposeView(graph: graph)
            .ignoresSafeArea(.all)
    }
}
