import Foundation
import ComposeApp

/// Caso síncrono da fronteira: responde na hora, não há recurso a desligar.
final class SwiftGreetingEngine: NSObject, GreetingEngine {
    var isAvailable: Bool { true }

    func greeting(name: String) -> String {
        "Olá, \(name)"
    }
}
