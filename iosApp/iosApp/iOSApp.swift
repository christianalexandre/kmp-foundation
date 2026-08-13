import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    /// Dono do composition root no iOS — ADR 0003.
    private let graph = AppGraph(
        greetingEngine: SwiftGreetingEngine(),
        deviceScanner: SwiftDeviceScanner()
    )

    var body: some Scene {
        WindowGroup {
            // RAIZ TROCADA PELA BANCADA DO RFC 0001 enquanto o spike está em
            // validação — qualquer run (Xcode, Android Studio, make ios-run) cai
            // aqui. Para voltar ao app: trocar por `ContentView(graph: graph)`.
            SpikeRootView()
        }
    }
}
