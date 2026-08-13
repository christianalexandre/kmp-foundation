package com.kmpfoundation.androidapp.spike

import android.os.Debug
import android.view.Choreographer
import kotlin.math.max

/**
 * Instrumentação da bancada do RFC 0001 no Android — par do
 * `iosApp/iosApp/Spike/SpikeMetrics.swift`. Não é código de produto: sai inteira
 * quando a medição virar ADR.
 *
 * **Aviso sobre comparar plataformas:** `totalPss` (aqui) e `phys_footprint` (iOS)
 * medem coisas diferentes e **não são comparáveis em valor absoluto**. Só os
 * *deltas* — o custo marginal de mais um destino — dizem algo entre as duas, e
 * ainda assim como ordem de grandeza. Os critérios de decisão do RFC são por
 * plataforma, não um número único.
 */
object SpikeMetrics {

    private const val HISTORY_LIMIT = 6
    private const val BYTES_PER_KB = 1024L
    private const val BYTES_PER_MB = 1024.0 * 1024.0

    private val firstFrames = mutableListOf<String>()
    private val memoryByDepth = linkedMapOf<Int, Long>()

    /** Profundidades marcadas com `*` no relatório: destinos sem Compose. */
    private val nativeDepths = mutableSetOf<Int>()

    private var pushStartedAtNanos = 0L

    var lastBackSummary: String = "—"
        private set

    /**
     * PSS do próprio processo. É a métrica padrão de "quanta memória meu app
     * usa" no Android — e é **cara** (dezenas de ms). Por isso nunca é lida
     * entre o início do push e o carimbo do primeiro frame: contaminaria
     * justamente o número que a medição quer.
     */
    fun pssBytes(): Long {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        return info.totalPss.toLong() * BYTES_PER_KB
    }

    fun beginPush() {
        pushStartedAtNanos = System.nanoTime()
    }

    fun recordFirstDraw(depth: Int, drawnAtNanos: Long) {
        if (pushStartedAtNanos == 0L) return
        val elapsedMs = (drawnAtNanos - pushStartedAtNanos) / NANOS_PER_MS
        pushStartedAtNanos = 0L

        firstFrames += "d$depth${marker(depth)} ${elapsedMs.format0()} ms"
        if (firstFrames.size > HISTORY_LIMIT) {
            firstFrames.removeAt(0)
        }
    }

    /**
     * Amostra de memória **com a tela já assentada** — não no push.
     *
     * Medido como delta instantâneo (antes/depois do push, ~150 ms), o número dá
     * negativo: nessa janela o PSS oscila por GC e por páginas compartilhadas mais
     * do que o custo da raiz nova. O sinal está na **inclinação** entre
     * profundidades depois que tudo assentou, não em cada delta isolado.
     */
    fun recordSettledMemory(depth: Int) {
        memoryByDepth[depth] = pssBytes()
    }

    fun markNative(depth: Int) {
        nativeDepths += depth
    }

    private fun marker(depth: Int): String = if (depth in nativeDepths) "*" else ""

    fun recordBack(stats: SpikeFrameStats) {
        lastBackSummary = stats.summary
    }

    fun report(depth: Int): String = buildString {
        appendLine("RFC 0001 · spike de N raízes Compose (Android)   * = destino nativo")
        appendLine("profundidade $depth")
        appendLine(
            if (firstFrames.isEmpty()) {
                "1º frame: empilhe destinos para medir"
            } else {
                "1º frame por push: ${firstFrames.joinToString(" · ")}"
            },
        )
        appendLine("memória assentada: ${memorySeries()}")
        append("back preditivo: $lastBackSummary")
    }

    /** A inclinação desta série é o custo por raiz. */
    private fun memorySeries(): String =
        if (memoryByDepth.isEmpty()) {
            "amostrando…"
        } else {
            memoryByDepth.entries.joinToString(" · ") { (depth, bytes) ->
                "d$depth${marker(depth)} ${formatMegabytes(bytes)}"
            }
        }

    fun formatMegabytes(bytes: Long): String = "${(bytes / BYTES_PER_MB).format1()} MB"
}

private const val NANOS_PER_MS = 1_000_000.0
private const val DROP_TOLERANCE = 1.5
private const val MS_PER_SECOND = 1000.0
private const val FALLBACK_REFRESH_HZ = 60f

private fun Double.format0(): String = "%.0f".format(this)

private fun Double.format1(): String = "%.1f".format(this)

data class SpikeFrameStats(
    val frameCount: Int,
    val elapsedSeconds: Double,
    val worstFrameMs: Double,
    val droppedFrames: Int,
) {
    private val averageFps: Double
        get() = if (elapsedSeconds > 0) frameCount / elapsedSeconds else 0.0

    val summary: String
        get() = if (frameCount == 0) {
            "sem amostras (back preditivo indisponível neste device?)"
        } else {
            "${averageFps.format0()} fps médio · pior frame ${worstFrameMs.format1()} ms · " +
                "$droppedFrames atrasados de $frameCount"
        }
}

/**
 * Amostra a duração de cada frame durante o **back preditivo** — o par Android do
 * pop interativo do iOS, e o mesmo pior caso: duas Activities (duas raízes
 * Compose) desenhando ao mesmo tempo enquanto o dedo se move.
 */
class SpikeFrameSampler(refreshRateHz: Float) {

    private val budgetMs = MS_PER_SECOND / max(refreshRateHz, FALLBACK_REFRESH_HZ)

    private var running = false
    private var lastNanos = 0L
    private var startNanos = 0L
    private var frameCount = 0
    private var worstFrameMs = 0.0
    private var droppedFrames = 0

    val isRunning: Boolean get() = running

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastNanos > 0L) {
                val frameMs = (frameTimeNanos - lastNanos) / NANOS_PER_MS
                frameCount++
                worstFrameMs = max(worstFrameMs, frameMs)
                if (frameMs > budgetMs * DROP_TOLERANCE) {
                    droppedFrames++
                }
            }
            lastNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastNanos = 0L
        frameCount = 0
        worstFrameMs = 0.0
        droppedFrames = 0
        startNanos = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop(): SpikeFrameStats {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        return SpikeFrameStats(
            frameCount = frameCount,
            elapsedSeconds = (System.nanoTime() - startNanos) / NANOS_PER_MS / MS_PER_SECOND,
            worstFrameMs = worstFrameMs,
            droppedFrames = droppedFrames,
        )
    }
}
