import SwiftUI
import UIKit
import ComposeApp

/// Hospeda a raiz Compose (MainViewController exportado no framework ComposeApp).
/// Dependências de plataforma (engines nativas etc.) são criadas aqui e passadas
/// como parâmetros de MainViewController — padrão do imou-smart-home.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
