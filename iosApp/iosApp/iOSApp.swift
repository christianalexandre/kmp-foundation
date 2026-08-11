import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    /// Dono do composition root no iOS — ver ADR 0003. Vive enquanto o app
    /// vive; criá-lo dentro do `ComposeView` o reconstruiria a cada vez que a
    /// SwiftUI recriasse a view.
    private let graph = AppGraph(engine: SwiftGreetingEngine())

    var body: some Scene {
        WindowGroup {
            ContentView(graph: graph)
        }
    }
}
