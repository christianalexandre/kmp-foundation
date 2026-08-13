package com.kmpfoundation.nav

import kotlinx.serialization.Serializable

/**
 * Destinos do app.
 *
 * Carregam **ids, nunca objetos de domínio**: a rota precisa ser reconstruível a
 * partir de um `Intent` restaurado depois de morte de processo — e, quando
 * existirem, de um deeplink ou de um push — sem depender do que já está carregado
 * em memória (ADR 0007). O caminho exercitado hoje é o do `Intent`; deeplink e
 * push são dívida registrada, com gatilho.
 *
 * Este pacote **não conhece feature nenhuma** (só ids atravessam), o que o mantém
 * livre do ciclo que o SDD 02 proíbe. Quem liga rota a tela é `ui/`, que é o topo.
 *
 * Não existe pilha aqui. A pilha é a da plataforma — `UINavigationController` no
 * iOS, pilha de Activities no Android. Sem espelho, não há o que sincronizar.
 */
@Serializable
sealed interface Route {

    @Serializable
    data object Devices : Route

    /**
     * Desenhada pela **plataforma**, não pelo Compose — não existe composable para
     * ela. É a intercalação `Compose → nativo → Compose` que o ADR 0007 decidiu
     * suportar, e o exemplo de tela que um SDK entregaria pronta.
     */
    @Serializable
    data class DeviceDetail(val deviceId: String) : Route

    /**
     * Alcançável por **duas entradas**: direto da lista (Compose → Compose) e a
     * partir da tela nativa (Compose por cima do nativo). É a mesma rota nos dois
     * casos, e a casca não distingue quem a empurrou — que é o ponto do ADR 0007.
     */
    @Serializable
    data class DeviceSettings(val deviceId: String) : Route

    /** `true` quando quem desenha é a plataforma. A casca lê isto para saber o que montar. */
    val isNative: Boolean
        get() = this is DeviceDetail
}
