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
    var isAvailable: Bool { true }

    /// Sem estado de chamada na engine: o handle devolvido é o dono do
    /// `DispatchWorkItem`, então cancelar uma chamada não afeta as outras
    /// (ADR 0004).
    func greet(name: String, callback: GreetingCallback) -> Cancellable {
        let device = UIDevice.current
        let work = DispatchWorkItem {
            callback.onResult(
                greeting: "Olá, \(name) — de \(device.systemName) \(device.systemVersion)",
                errorMessage: nil
            )
        }
        DispatchQueue.main.async(execute: work)
        return WorkItemCancellable(work: work)
    }
}

private final class WorkItemCancellable: NSObject, Cancellable {
    private let work: DispatchWorkItem

    init(work: DispatchWorkItem) {
        self.work = work
    }

    func cancel() {
        work.cancel()
    }
}
