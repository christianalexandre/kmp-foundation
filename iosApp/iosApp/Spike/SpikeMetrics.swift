import Foundation
import QuartzCore
import UIKit

// Instrumentação do spike do RFC 0001. Não é código de produto — sai junto com o
// resto do spike quando a medição virar ADR.

// MARK: - Memória

enum SpikeMemory {

    /// `phys_footprint` é a métrica que o próprio iOS usa para decidir jetsam —
    /// diferente de `resident_size`, que conta páginas compartilhadas e infla o
    /// número. É a única que responde "quanto este destino custou de verdade".
    static func physFootprintBytes() -> UInt64 {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(
            MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size
        )
        let result = withUnsafeMutablePointer(to: &info) { pointer in
            pointer.withMemoryRebound(to: integer_t.self, capacity: Int(count)) { rebound in
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), rebound, &count)
            }
        }
        guard result == KERN_SUCCESS else { return 0 }
        return info.phys_footprint
    }

    static func formatMegabytes(_ bytes: UInt64) -> String {
        String(format: "%.1f MB", Double(bytes) / 1024.0 / 1024.0)
    }
}

// MARK: - Frame rate

struct SpikeFrameStats {
    let frameCount: Int
    let elapsed: CFTimeInterval
    let worstFrameMs: Double
    let droppedFrames: Int

    var averageFps: Double {
        guard elapsed > 0 else { return 0 }
        return Double(frameCount) / elapsed
    }

    var summary: String {
        guard frameCount > 0 else { return "sem amostras" }
        return String(
            format: "%.0f fps médio · pior frame %.1f ms · %d atrasados de %d",
            averageFps, worstFrameMs, droppedFrames, frameCount
        )
    }
}

/// Amostra a duração de cada frame via `CADisplayLink` enquanto o pop interativo
/// acontece. É o pior caso do desenho da opção 3: duas composições Compose vivas
/// renderizando ao mesmo tempo, seguindo o dedo.
final class SpikeFrameSampler {

    private var displayLink: CADisplayLink?
    private var lastTimestamp: CFTimeInterval = 0
    private var startTimestamp: CFTimeInterval = 0
    private var frameCount = 0
    private var worstFrameMs: Double = 0
    private var droppedFrames = 0
    private var budgetMs: Double = 0

    var isRunning: Bool { displayLink != nil }

    func start() {
        guard displayLink == nil else { return }
        frameCount = 0
        worstFrameMs = 0
        droppedFrames = 0
        lastTimestamp = 0
        startTimestamp = CACurrentMediaTime()

        // ProMotion: o orçamento por frame é 1/120 s quando o device suporta, e o
        // Info.plist já habilita 120 Hz (CADisableMinimumFrameDurationOnPhone).
        let maxFps = max(UIScreen.main.maximumFramesPerSecond, 60)
        budgetMs = 1000.0 / Double(maxFps)

        let link = CADisplayLink(target: self, selector: #selector(onFrame(_:)))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    func stop() -> SpikeFrameStats {
        displayLink?.invalidate()
        displayLink = nil
        return SpikeFrameStats(
            frameCount: frameCount,
            elapsed: CACurrentMediaTime() - startTimestamp,
            worstFrameMs: worstFrameMs,
            droppedFrames: droppedFrames
        )
    }

    @objc
    private func onFrame(_ link: CADisplayLink) {
        defer { lastTimestamp = link.timestamp }
        guard lastTimestamp > 0 else { return }

        let frameMs = (link.timestamp - lastTimestamp) * 1000.0
        frameCount += 1
        worstFrameMs = max(worstFrameMs, frameMs)
        // Tolerância de 50%: variação pequena é ruído do próprio CADisplayLink.
        if frameMs > budgetMs * 1.5 {
            droppedFrames += 1
        }
    }
}
