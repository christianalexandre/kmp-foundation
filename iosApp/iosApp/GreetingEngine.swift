import Foundation
import UIKit
import ComposeApp

/// Implementação Swift de um protocolo declarado em Kotlin (`commonMain`).
///
/// É o padrão que o app real usará para os SDKs nativos: o contrato vive no
/// código compartilhado, a implementação vive onde o SDK vive. Só funciona
/// porque a interface Kotlin evita `suspend` e tipos selados na assinatura —
/// nenhum dos dois sobrevive à exportação para o header Objective-C.
final class SwiftGreetingEngine: NSObject, GreetingEngine {
    private var work: DispatchWorkItem?

    var isAvailable: Bool { true }

    func greet(name: String, callback: GreetingCallback) {
        let device = UIDevice.current
        let work = DispatchWorkItem {
            callback.onResult(
                greeting: "Olá, \(name) — de \(device.systemName) \(device.systemVersion)",
                errorMessage: nil
            )
        }
        self.work = work
        DispatchQueue.main.async(execute: work)
    }

    func cancel() {
        work?.cancel()
        work = nil
    }
}
