import Foundation
import ComposeApp

/// Implementação Swift de um protocolo declarado em Kotlin (`commonMain`) — é o
/// padrão que o app real usará para os SDKs nativos.
final class SwiftGreetingEngine: NSObject, GreetingEngine {
    var isAvailable: Bool { true }

    func greet(name: String, callback: GreetingCallback) -> Cancellable {
        let work = DispatchWorkItem {
            callback.onResult(greeting: "Olá, \(name)", errorMessage: nil)
        }
        DispatchQueue.main.async(execute: work)
        // O handle retém o work item; a engine não guarda estado de chamada (ADR 0004).
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
