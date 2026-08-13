package com.kmpfoundation.androidapp.spike

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.kmpfoundation.ui.spike.SpikeScreen

/**
 * Bancada do RFC 0001 no Android — par do `SpikeCoordinator` do iOS.
 *
 * **Uma Activity por destino**, cada uma com sua própria raiz Compose: é a forma
 * que a opção 3 do RFC assume no Android, do mesmo jeito que um
 * `ComposeUIViewController` por destino é a forma dela no iOS. Roda o **mesmo**
 * `SpikeScreen` de `commonMain` que a bancada iOS, para que a diferença medida
 * seja de plataforma e não de conteúdo.
 *
 * Não é código de produto: sai inteira quando a medição virar ADR.
 */
class SpikeActivity : ComponentActivity() {

    private lateinit var hud: TextView
    private lateinit var sampler: SpikeFrameSampler

    private var depth = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depth = intent.getIntExtra(EXTRA_DEPTH, 1)
        sampler = SpikeFrameSampler(currentRefreshRate())

        setContent {
            // Insets tratados aqui, na casca, e não no SpikeScreen: o screen é
            // compartilhado com o iOS, onde quem inseta é o próprio view controller
            // (edgesForExtendedLayout). Cada plataforma resolve o seu.
            Box(modifier = Modifier.safeDrawingPadding()) {
                SpikeScreen(
                    depth = depth,
                    onFirstDraw = {
                        // Carimba o relógio ANTES de qualquer outra coisa: a leitura
                        // de PSS que vem depois custa dezenas de ms e falsearia o número.
                        val drawnAtNanos = System.nanoTime()
                        SpikeMetrics.recordFirstDraw(depth, drawnAtNanos)
                        refreshHud()
                    },
                    onPushNext = ::pushNext,
                    onPushNative = ::pushNative,
                )
            }
        }

        installHud()
        installBackSampling()
    }

    override fun onResume() {
        super.onResume()
        refreshHud()
        // Amostra a memória só depois da transição assentar: no meio dela o número
        // é ruído (ver SpikeMetrics.recordSettledMemory).
        hud.postDelayed(
            {
                SpikeMetrics.recordSettledMemory(depth)
                refreshHud()
            },
            SETTLE_DELAY_MS,
        )
    }

    private fun pushNext() {
        SpikeMetrics.beginPush()
        startActivity(
            Intent(this, SpikeActivity::class.java)
                .putExtra(EXTRA_DEPTH, depth + 1),
        )
    }

    /**
     * Empilha um destino **sem Compose** na mesma pilha de Activities. É o que
     * prova a intercalação: dele sai outro destino Compose, formando o sanduíche
     * Compose → nativo → Compose.
     */
    private fun pushNative() {
        SpikeMetrics.beginPush()
        startActivity(
            Intent(this, SpikeNativeActivity::class.java)
                .putExtra(EXTRA_DEPTH, depth + 1),
        )
    }

    // HUD fora do Compose, como no iOS: não entra na composição que está sendo medida.
    private fun installHud() {
        hud = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(HUD_BACKGROUND)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, HUD_TEXT_SIZE_SP)
            typeface = Typeface.MONOSPACE
            // Folga extra embaixo para o HUD não ficar sob a barra de gestos.
            setPadding(HUD_PADDING, HUD_PADDING, HUD_PADDING, HUD_PADDING_BOTTOM)
        }
        addContentView(
            hud,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )
        refreshHud()
    }

    private fun refreshHud() {
        hud.text = SpikeMetrics.report(depth)
    }

    /**
     * O back **preditivo** é a janela de amostragem do pior caso: durante o gesto,
     * esta Activity e a anterior desenham ao mesmo tempo — duas raízes Compose
     * vivas, que é exatamente o que o pop interativo do iOS provoca.
     */
    private fun installBackSampling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    sampler.start()
                }

                override fun handleOnBackCancelled() {
                    finishSampling()
                }

                override fun handleOnBackPressed() {
                    finishSampling()
                    // Desliga este callback e delega: sem isto o back não sai da tela.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            },
        )
    }

    private fun finishSampling() {
        if (!sampler.isRunning) {
            // `handleOnBackStarted` só dispara com back preditivo ativo (API 34+ e
            // `enableOnBackInvokedCallback` no manifesto). Sem ele não existe gesto
            // progressivo para medir, e o relatório precisa dizer isso.
            SpikeMetrics.recordBack(BACK_NOT_SAMPLED)
            return
        }
        SpikeMetrics.recordBack(sampler.stop())
    }

    private fun currentRefreshRate(): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.refreshRate ?: DEFAULT_REFRESH_HZ
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }

    companion object {
        const val EXTRA_DEPTH = "depth"

        private const val HUD_BACKGROUND = 0xCC000000.toInt()
        private const val HUD_TEXT_SIZE_SP = 11f
        private const val HUD_PADDING = 12
        private const val HUD_PADDING_BOTTOM = 56
        private const val DEFAULT_REFRESH_HZ = 60f
        private const val SETTLE_DELAY_MS = 800L

        private val BACK_NOT_SAMPLED = SpikeFrameStats(
            frameCount = 0,
            elapsedSeconds = 0.0,
            worstFrameMs = 0.0,
            droppedFrames = 0,
        )
    }
}
