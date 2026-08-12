import Foundation
import ComposeApp

/// Simula uma busca de dispositivos: acha um a cada intervalo até o limite.
/// O SDK real (BLE, mDNS) entra exatamente aqui.
///
/// O `Timer` é recurso de verdade — segue disparando até alguém invalidá-lo.
/// É o que torna este exemplo honesto: o `cancel()` tem o que desligar.
///
/// O contrato pede entrega de callback serializada; o Timer agendado no
/// RunLoop principal já garante isso. O trabalho de um SDK real roda nas
/// threads dele — só a entrega é marshalada.
final class SwiftDeviceScanner: NSObject, DeviceScanner {
    private static let intervalSeconds = 0.8
    private static let maxDevices = 5

    var isAvailable: Bool { true }

    func startScan(callback: DeviceScanCallback) -> Cancellable {
        var found = 0

        let timer = Timer.scheduledTimer(
            withTimeInterval: Self.intervalSeconds,
            repeats: true
        ) { timer in
            found += 1
            callback.onDeviceFound(id: "ios-\(found)", name: "Câmera \(found)")
            if found >= Self.maxDevices {
                timer.invalidate()
                callback.onFinished(errorMessage: nil)
            }
        }

        return CallHandle(onCancel: { timer.invalidate() })
    }
}
