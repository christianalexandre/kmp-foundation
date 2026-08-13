import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    /// Dono do composition root no iOS — ADR 0003.
    private let graph = AppGraph(deviceScanner: SwiftDeviceScanner())

    var body: some Scene {
        WindowGroup {
            ContentView(graph: graph)
        }
    }
}
