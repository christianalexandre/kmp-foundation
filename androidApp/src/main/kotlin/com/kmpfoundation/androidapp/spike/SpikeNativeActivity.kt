package com.kmpfoundation.androidapp.spike

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Destino **100% nativo** da bancada do RFC 0001: UI de `View`, sem uma linha de
 * Compose. Par do `SpikeNativeView.swift`.
 *
 * É a peça que valida a **intercalação** — dela sai outro destino Compose,
 * formando o sanduíche Compose → nativo → Compose. Sem ela o spike mediria o
 * custo do desenho sem provar que o desenho entrega o que prometeu: uma tela
 * nativa como destino de topo com Compose empilhado por cima.
 *
 * Não é código de produto: sai inteira quando a medição virar ADR.
 */
class SpikeNativeActivity : ComponentActivity() {

    private lateinit var hud: TextView

    private var depth = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depth = intent.getIntExtra(SpikeActivity.EXTRA_DEPTH, 1)
        SpikeMetrics.markNative(depth)

        val root = buildContent()
        setContentView(root)
        // HUD antes do listener: é ele que o callback de primeiro desenho atualiza.
        installHud()
        reportFirstDraw(root)
    }

    override fun onResume() {
        super.onResume()
        refreshHud()
        root()?.postDelayed(
            {
                SpikeMetrics.recordSettledMemory(depth)
                refreshHud()
            },
            SETTLE_DELAY_MS,
        )
    }

    private fun root(): View? = findViewById(android.R.id.content)

    /** Mesmo HUD do destino Compose: sem ele, as métricas somem justamente aqui. */
    private fun installHud() {
        hud = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(HUD_BACKGROUND)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, HUD_TEXT_SIZE_SP)
            typeface = Typeface.MONOSPACE
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

    private fun buildContent(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(PADDING, PADDING, PADDING, PADDING)
        }

        column.addView(
            TextView(this).apply {
                text = "Destino $depth — View nativa, sem Compose"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SIZE_SP)
                setPadding(0, 0, 0, PADDING)
            },
        )
        column.addView(
            Button(this).apply {
                text = "Empilhar Compose ${depth + 1}"
                setOnClickListener { pushCompose() }
            },
        )

        // Mesmo volume de conteúdo do SpikeScreen: comparar uma tela nativa vazia
        // com uma lista Compose responderia a pergunta errada.
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        repeat(ROWS) { index ->
            list.addView(
                TextView(this).apply {
                    text = "Ajuste ${index + 1} do destino $depth\n" +
                        "Descrição de apoio para dar ao texto um volume realista de layout"
                    setPadding(0, ROW_PADDING, 0, ROW_PADDING)
                },
            )
        }
        column.addView(
            ScrollView(this).apply {
                addView(list)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        return LinearLayout(this).apply {
            gravity = Gravity.TOP
            addView(
                column,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            fitsSystemWindows = true
        }
    }

    /**
     * Primeiro desenho de verdade, para o número ser comparável ao que o Compose
     * reporta via `drawWithContent`. O listener é removido por `post` porque
     * removê-lo de dentro do próprio callback lança.
     */
    private fun reportFirstDraw(root: View) {
        var reported = false
        lateinit var listener: ViewTreeObserver.OnDrawListener
        listener = ViewTreeObserver.OnDrawListener {
            if (reported) return@OnDrawListener
            reported = true
            SpikeMetrics.recordFirstDraw(depth, System.nanoTime())
            root.post {
                root.viewTreeObserver.removeOnDrawListener(listener)
                refreshHud()
            }
        }
        root.viewTreeObserver.addOnDrawListener(listener)
    }

    private fun pushCompose() {
        SpikeMetrics.beginPush()
        startActivity(
            Intent(this, SpikeActivity::class.java)
                .putExtra(SpikeActivity.EXTRA_DEPTH, depth + 1),
        )
    }

    private companion object {
        const val PADDING = 32
        const val ROW_PADDING = 16
        const val ROWS = 40
        const val TITLE_SIZE_SP = 18f
        const val SETTLE_DELAY_MS = 800L
        const val HUD_BACKGROUND = 0xCC000000.toInt()
        const val HUD_TEXT_SIZE_SP = 11f
        const val HUD_PADDING = 12
        const val HUD_PADDING_BOTTOM = 56
    }
}
