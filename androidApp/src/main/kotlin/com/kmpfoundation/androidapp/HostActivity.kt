package com.kmpfoundation.androidapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kmpfoundation.nav.NavigationPolicy
import com.kmpfoundation.nav.Route
import com.kmpfoundation.nav.RouteCodec
import com.kmpfoundation.ui.RouteContent

/**
 * Hospeda **um** destino Compose — ADR 0007.
 *
 * É uma classe só, com várias instâncias: cada uma lê a sua rota do próprio
 * `Intent` e desenha. Não é "uma Activity por tela" no sentido de escrever vinte
 * Activities; é um host, empilhado quantas vezes a navegação pedir.
 *
 * O modelo de Activity única não estava disponível aqui: telas de SDK **são**
 * Activities, então o app é multi-Activity no instante em que uma delas abre. E
 * uma tela Compose por cima de uma tela nativa é impossível com raiz única, que é
 * justamente o caso que o ADR 0007 precisa suportar.
 *
 * De brinde, a navegação sobrevive à morte de processo sem código nosso: o
 * sistema restaura a task com os `Intent` intactos, e cada host relê sua rota.
 */
class HostActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val graph = (application as KmpFoundationApplication).graph
        // Intent de versão antiga do app, ou deeplink malformado: cai no início em
        // vez de crashar. Quem decide o fallback é o comum, não a casca.
        val route = RouteCodec.decodeOrNull(intent.getStringExtra(EXTRA_ROUTE))
            ?: NavigationPolicy.start

        setContent {
            RouteContent(
                route = route,
                graph = graph,
                onNavigate = ::navigate,
                // A raiz da task não tem para onde voltar — e, por decisão de
                // produto, é a única tela sem cabeçalho. Quem sabe se é raiz é a
                // casca, porque a pilha é dela (ADR 0007).
                onBack = if (isTaskRoot) null else ::finish,
            )
        }
    }

    /**
     * A casca sabe **como** apresentar, nunca **o quê**: a rota já chegou decidida
     * pela [NavigationPolicy]. Destino nativo vira outra Activity; destino Compose
     * vira outra instância desta.
     */
    private fun navigate(to: Route) {
        val intent = if (to.isNative) {
            DeviceDetailActivity.newIntent(this, to)
        } else {
            newIntent(this, to)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ROUTE = "route"

        fun newIntent(context: Context, route: Route): Intent =
            Intent(context, HostActivity::class.java)
                .putExtra(EXTRA_ROUTE, RouteCodec.encode(route))
    }
}
