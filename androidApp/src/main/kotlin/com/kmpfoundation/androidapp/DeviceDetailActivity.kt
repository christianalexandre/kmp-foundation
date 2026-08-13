package com.kmpfoundation.androidapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.kmpfoundation.nav.NavigationPolicy
import com.kmpfoundation.nav.Route
import com.kmpfoundation.nav.RouteCodec
import com.kmpfoundation.nav.titleOf

/**
 * Destino **100% nativo**: UI de `View`, sem uma linha de Compose. Par do
 * `DeviceDetailView.swift`.
 *
 * É o exemplo do que o ADR 0007 existe para suportar — a tela que um SDK entrega
 * pronta (pareamento, preview de câmera, player) e que não vale a pena embrulhar
 * em Compose. Dela sai outro destino Compose, que é o caso de tela nativa como
 * hub e o que torna raiz Compose única inviável.
 *
 * O cabeçalho é desenhado em `View` aqui, e o texto vem de `titleOf` — o mesmo que
 * o Compose e o iOS usam. A tela nativa desenha o cabeçalho, não decide o que ele
 * diz. A costura aparece, e num app real apareceria igual: a tela é do SDK.
 *
 * Exemplo do template: apagar junto com `features/devices` ao clonar.
 */
class DeviceDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Como no host Compose: com targetSdk 35+ o edge-to-edge é forçado de todo
        // jeito, e sem isto os ícones da status bar ficam claros sobre fundo claro.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val route = RouteCodec.decodeOrNull(intent.getStringExtra(HostActivity.EXTRA_ROUTE))
        val detail = route as? Route.DeviceDetail
        if (detail == null) {
            finish()
            return
        }

        setContentView(content(detail))
    }

    private fun content(route: Route.DeviceDetail): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(PADDING, PADDING, PADDING, PADDING)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // Com targetSdk 35+ o sistema força edge-to-edge, e `fitsSystemWindows`
            // não basta: sem consumir os insets, o conteúdo fica sob a status bar.
            // A casca trata isto — do lado Compose é o `safeDrawingPadding`.
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = systemBarsInsets(insets)
                view.setPadding(PADDING, PADDING + bars.first, PADDING, PADDING + bars.second)
                insets
            }
        }

        column.addView(header(titleOf(route)))
        column.addView(
            TextView(this).apply {
                text = "Tela nativa"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADING_SP)
            },
        )
        column.addView(
            TextView(this).apply {
                text = "Dispositivo ${route.deviceId}\n\nDesenhada em View, empilhada na " +
                    "mesma pilha de Activities das telas Compose."
                setPadding(0, PADDING, 0, PADDING)
            },
        )
        column.addView(
            Button(this).apply {
                text = "Abrir ajustes (volta para o Compose)"
                setOnClickListener {
                    startActivity(
                        HostActivity.newIntent(
                            this@DeviceDetailActivity,
                            NavigationPolicy.onDeviceSettings(route.deviceId),
                        ),
                    )
                }
            },
        )
        return column
    }

    /** Espelha o cabeçalho que o `RouteContent` desenha em Compose. */
    private fun header(title: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(
            Button(this@DeviceDetailActivity).apply {
                text = "Voltar"
                setOnClickListener { finish() }
            },
        )
        addView(
            TextView(this@DeviceDetailActivity).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP)
            },
        )
    }

    /** @return topo e base das barras do sistema. */
    private fun systemBarsInsets(insets: WindowInsets): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            bars.top to bars.bottom
        } else {
            @Suppress("DEPRECATION")
            insets.systemWindowInsetTop to insets.systemWindowInsetBottom
        }

    companion object {
        private const val PADDING = 48
        private const val TITLE_SP = 22f
        private const val HEADING_SP = 20f

        fun newIntent(context: Context, route: Route): Intent =
            Intent(context, DeviceDetailActivity::class.java)
                .putExtra(HostActivity.EXTRA_ROUTE, RouteCodec.encode(route))
    }
}
