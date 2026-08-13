import ComposeApp
import QuartzCore
import SwiftUI
import UIKit

/// Bancada do spike do RFC 0001, questão 1: **quanto custa uma raiz Compose por
/// destino no iOS?** É a forma que a opção 3 do RFC exige, e o número não existe
/// em nenhum dos dois repositórios.
///
/// Mede três coisas, todas em `UINavigationController` real:
/// 1. `phys_footprint` acrescentado **por destino empilhado** (custo marginal);
/// 2. tempo entre `pushViewController` e o **primeiro desenho** do Compose;
/// 3. frame rate durante o **pop interativo** — duas composições vivas ao mesmo
///    tempo, que é o pior caso do desenho e o que decide a paridade de swipe-back.
///
/// Não é código de produto: sai inteiro quando a medição virar ADR.
final class SpikeCoordinator: NSObject, UINavigationControllerDelegate, UIGestureRecognizerDelegate {

    private weak var navigationController: UINavigationController?
    private let overlay = UILabel()
    private let sampler = SpikeFrameSampler()

    private var pushStartedAt: CFTimeInterval = 0
    private var firstFrames: [String] = []
    private var memoryByDepth: [(depth: Int, bytes: UInt64)] = []
    /// Profundidades marcadas com `*` no relatório: destinos sem Compose.
    private var nativeDepths: Set<Int> = []
    private var lastPopSummary = "—"

    private let historyLimit = 6
    /// Espera a transição assentar antes de amostrar memória — ver `sampleSettledMemory`.
    private let settleDelay: TimeInterval = 0.8

    // MARK: - Montagem

    func makeNavigationController() -> UINavigationController {
        let nav = UINavigationController(rootViewController: makeDestination(depth: 1))
        nav.delegate = self
        navigationController = nav
        installOverlay(on: nav)
        // Alvo adicional no gesto de borda do UIKit: é ele que abre e fecha a
        // janela de amostragem do pop interativo.
        nav.interactivePopGestureRecognizer?.addTarget(self, action: #selector(onPopGesture(_:)))
        return nav
    }

    private func installOverlay(on nav: UINavigationController) {
        overlay.numberOfLines = 0
        overlay.font = .monospacedSystemFont(ofSize: 11, weight: .regular)
        overlay.textColor = .white
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.8)
        overlay.isUserInteractionEnabled = false
        overlay.translatesAutoresizingMaskIntoConstraints = false
        nav.view.addSubview(overlay)
        NSLayoutConstraint.activate([
            overlay.leadingAnchor.constraint(equalTo: nav.view.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: nav.view.trailingAnchor),
            overlay.bottomAnchor.constraint(equalTo: nav.view.safeAreaLayoutGuide.bottomAnchor)
        ])
        refreshOverlay()
    }

    // MARK: - Empilhar destinos

    private func makeDestination(depth: Int) -> UIViewController {
        let controller = SpikeViewControllerKt.SpikeViewController(
            depth: Int32(depth),
            onFirstDraw: { [weak self] in
                // Carimba o instante ANTES de qualquer trabalho de UI: o Compose
                // desenha na main thread, e um hop de runloop aqui falsearia a
                // medição justamente no número que interessa.
                let drawnAt = CACurrentMediaTime()
                self?.recordFirstDraw(at: drawnAt)
            },
            onPushNext: { [weak self] in
                self?.pushNext()
            },
            onPushNative: { [weak self] in
                self?.pushNative()
            }
        )
        controller.title = "Destino \(depth)"
        // Sem isto o Compose se estende por baixo da nav bar e o título fica em
        // cima do conteúdo. A nav bar precisa estar em cena (é ela que dá a
        // transição de push real), então o conteúdo é que cede.
        controller.edgesForExtendedLayout = []
        return controller
    }

    private func pushNext() {
        guard let nav = navigationController else { return }
        let depth = nav.viewControllers.count + 1
        pushStartedAt = CACurrentMediaTime()
        nav.pushViewController(makeDestination(depth: depth), animated: true)
    }

    /// Empilha um destino **sem Compose** (SwiftUI puro) no MESMO
    /// `UINavigationController`. É o que prova a intercalação: a tela nativa entra
    /// no meio da pilha, e dela sai outro destino Compose.
    private func pushNative() {
        guard let nav = navigationController else { return }
        let depth = nav.viewControllers.count + 1
        nativeDepths.insert(depth)
        pushStartedAt = CACurrentMediaTime()

        let native = SpikeNativeView(
            depth: depth,
            onFirstDraw: { [weak self] in
                let drawnAt = CACurrentMediaTime()
                self?.recordFirstDraw(at: drawnAt)
            },
            onPushCompose: { [weak self] in
                self?.pushNext()
            }
        )
        let host = UIHostingController(rootView: native)
        host.title = "Destino \(depth) (nativo)"
        nav.pushViewController(host, animated: true)
    }

    private func recordFirstDraw(at drawnAt: CFTimeInterval) {
        guard pushStartedAt > 0 else { return }
        let elapsedMs = (drawnAt - pushStartedAt) * 1000.0
        pushStartedAt = 0

        let depth = navigationController?.viewControllers.count ?? 0
        firstFrames.append(String(format: "d%d%@ %.0f ms", depth, marker(for: depth), elapsedMs))
        if firstFrames.count > historyLimit {
            firstFrames.removeFirst()
        }
        refreshOverlay()
    }

    /// Amostra de memória **com a tela já assentada**, nunca no push.
    ///
    /// Medido como delta instantâneo em volta do push, o número dá negativo: nessa
    /// janela o `phys_footprint` oscila (alocação preguiçosa, liberação do que a
    /// transição usou) mais do que o custo da raiz nova. O sinal está na
    /// **inclinação** entre profundidades depois que tudo assentou.
    private func sampleSettledMemory() {
        let depth = navigationController?.viewControllers.count ?? 0
        guard depth > 0 else { return }
        let bytes = SpikeMemory.physFootprintBytes()
        if let index = memoryByDepth.firstIndex(where: { $0.depth == depth }) {
            memoryByDepth[index] = (depth, bytes)
        } else {
            memoryByDepth.append((depth, bytes))
        }
        refreshOverlay()
    }

    // MARK: - Pop interativo

    @objc
    private func onPopGesture(_ recognizer: UIGestureRecognizer) {
        switch recognizer.state {
        case .began:
            sampler.start()
        case .ended, .cancelled, .failed:
            guard sampler.isRunning else { return }
            lastPopSummary = sampler.stop().summary
            refreshOverlay()
        default:
            break
        }
    }

    // MARK: - UINavigationControllerDelegate

    func navigationController(
        _ navigationController: UINavigationController,
        didShow viewController: UIViewController,
        animated: Bool
    ) {
        navigationController.view.bringSubviewToFront(overlay)
        refreshOverlay()
        DispatchQueue.main.asyncAfter(deadline: .now() + settleDelay) { [weak self] in
            self?.sampleSettledMemory()
        }
    }

    // MARK: - Relatório

    private func refreshOverlay() {
        let depth = navigationController?.viewControllers.count ?? 0
        let frames = firstFrames.isEmpty
            ? "1º frame: empilhe destinos para medir"
            : "1º frame por push: " + firstFrames.joined(separator: " · ")
        // A inclinação desta série é o custo por raiz.
        let memory = memoryByDepth.isEmpty
            ? "amostrando…"
            : memoryByDepth
                .map { "d\($0.depth)\(marker(for: $0.depth)) \(SpikeMemory.formatMegabytes($0.bytes))" }
                .joined(separator: " · ")

        overlay.text = [
            "RFC 0001 · spike de N raízes Compose (iOS)   * = destino nativo",
            "profundidade \(depth)",
            frames,
            "memória assentada: \(memory)",
            "pop interativo: \(lastPopSummary)"
        ].joined(separator: "\n")
    }

    private func marker(for depth: Int) -> String {
        nativeDepths.contains(depth) ? "*" : ""
    }
}

/// Raiz do app quando o spike está ligado (argumento `--spike`).
struct SpikeRootView: UIViewControllerRepresentable {

    func makeCoordinator() -> SpikeCoordinator { SpikeCoordinator() }

    func makeUIViewController(context: Context) -> UINavigationController {
        context.coordinator.makeNavigationController()
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {}
}
