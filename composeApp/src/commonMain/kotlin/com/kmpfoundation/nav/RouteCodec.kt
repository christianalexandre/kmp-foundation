package com.kmpfoundation.nav

import kotlinx.serialization.json.Json

/**
 * Serializa a rota para atravessar coisas que só carregam texto: o extra do
 * `Intent` no Android — que é o que faz a navegação sobreviver à morte de
 * processo, já que o sistema restaura a task com os intents intactos — e, no
 * futuro, o payload de um push.
 *
 * Não é ponte para o Swift: no iOS a casca recebe a [Route] tipada, porque o
 * Kotlin/Native a exporta e não há nada a serializar entre objetos vivos.
 */
object RouteCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(route: Route): String = json.encodeToString(Route.serializer(), route)

    /**
     * @throws IllegalArgumentException quando o texto não é uma rota válida — que
     * é o caso de um deeplink malformado ou de um `Intent` de uma versão antiga do
     * app depois de atualizar. Quem chama decide o destino de fallback.
     */
    fun decode(text: String): Route = json.decodeFromString(Route.serializer(), text)

    /** Versão tolerante para as bordas: devolve `null` em vez de lançar. */
    fun decodeOrNull(text: String?): Route? =
        text?.let { runCatching { decode(it) }.getOrNull() }
}
