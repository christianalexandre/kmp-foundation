import SwiftUI

/// Destino **100% nativo**: SwiftUI puro, sem uma linha de Compose.
///
/// É o exemplo do que o ADR 0007 existe para suportar — a tela que um SDK entrega
/// pronta (pareamento, preview de câmera, player) e que não vale a pena embrulhar
/// em Compose. Dela sai outro destino Compose, que é o caso de tela nativa como
/// hub.
///
/// O cabeçalho é desenhado aqui, em SwiftUI, porque a nav bar do UIKit está
/// escondida (ver `RootCoordinator`). O **texto** vem de `titleOf`, em Kotlin, e é
/// o mesmo que o Android usa: a tela nativa desenha o cabeçalho, não decide o que
/// ele diz. A costura aparece — este cabeçalho é código separado do Compose — e
/// num app real ela apareceria igual, porque a tela é do SDK.
///
/// Exemplo do template: apagar junto com `features/devices` ao clonar.
struct DeviceDetailView: View {

    let deviceId: String
    let title: String
    let onOpenSettings: () -> Void
    let onBack: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let onBack {
                HStack(spacing: 8) {
                    Button("Voltar", action: onBack)
                    Text(title)
                        .font(.title2)
                }
                .frame(height: 56)
            }

            Text("Tela nativa")
                .font(.headline)
            Text("Dispositivo \(deviceId)")
                .foregroundStyle(.secondary)
            Text("Desenhada em SwiftUI, empilhada no mesmo UINavigationController "
                 + "das telas Compose. O swipe-back é o do UIKit nos dois casos.")
                .font(.footnote)
                .foregroundStyle(.secondary)

            Button("Abrir ajustes (volta para o Compose)", action: onOpenSettings)
                .buttonStyle(.borderedProminent)

            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
