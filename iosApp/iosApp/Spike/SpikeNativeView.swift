import SwiftUI
import UIKit

// Destino **100% nativo** da bancada do RFC 0001: SwiftUI puro, sem uma linha de
// Compose. É a peça que valida a intercalação — dele sai outro destino Compose,
// formando o sanduíche Compose → nativo → Compose. Sem isso o spike mediria o
// custo do desenho sem provar que o desenho entrega o que prometeu.

/// Reporta o **primeiro desenho** da tela nativa, para o número ser comparável ao
/// que o Compose reporta via `drawWithContent`.
///
/// Sobrescrever `draw(_:)` é o que faz o UIKit alocar backing store e chamá-lo —
/// uma `UIView` que não desenha nada normalmente nunca receberia essa chamada.
private final class FirstDrawReportingView: UIView {

    var onFirstDraw: (() -> Void)?
    private var reported = false

    override func draw(_ rect: CGRect) {
        super.draw(rect)
        guard !reported else { return }
        reported = true
        onFirstDraw?()
    }
}

private struct FirstDrawReporter: UIViewRepresentable {

    let onFirstDraw: () -> Void

    func makeUIView(context: Context) -> UIView {
        let view = FirstDrawReportingView()
        view.backgroundColor = .clear
        view.onFirstDraw = onFirstDraw
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}

struct SpikeNativeView: View {

    let depth: Int
    let onFirstDraw: () -> Void
    let onPushCompose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Destino \(depth) — SwiftUI, sem Compose")
                .font(.headline)
                .padding()

            Button("Empilhar Compose \(depth + 1)") {
                onPushCompose()
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
            .padding(.horizontal)

            List(1...rowCount, id: \.self) { index in
                VStack(alignment: .leading) {
                    Text("Ajuste \(index) do destino \(depth)")
                    Text("Descrição de apoio para dar ao texto um volume realista de layout")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .listStyle(.plain)
        }
        // Mesmo volume de conteúdo do SpikeScreen: comparar uma tela nativa vazia
        // com uma lista Compose responderia a pergunta errada.
        .background(FirstDrawReporter(onFirstDraw: onFirstDraw))
    }

    private var rowCount: Int { 40 }
}
