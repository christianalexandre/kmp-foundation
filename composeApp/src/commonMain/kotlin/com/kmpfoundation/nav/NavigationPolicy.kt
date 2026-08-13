package com.kmpfoundation.nav

/**
 * **Quem decide para onde ir.** A casca apresenta o que esta política mandar e
 * nunca escolhe destino — ADR 0007.
 *
 * A regra existe por dois motivos. O primeiro é que política de navegação
 * espalhada por três lugares (comum, Android, iOS) diverge em silêncio. O segundo
 * é este arquivo: funções puras, testáveis em `commonTest` sem device e sem
 * Compose, que é o que impede este fluxo de virar mais um buraco de cobertura.
 *
 * Se uma decisão de navegação não couber aqui como função pura, ela provavelmente
 * depende de estado — e aí o lugar dela é um ViewModel, não a casca.
 */
object NavigationPolicy {

    /** Primeira tela do app. A casca a usa quando não há rota a restaurar. */
    val start: Route = Route.Welcome

    fun onSeeDevices(): Route = Route.Devices

    /** Vai para uma tela **nativa** — a casca descobre isso por [Route.isNative]. */
    fun onDeviceSelected(deviceId: String): Route = Route.DeviceDetail(deviceId)

    /** Sai da tela nativa **de volta para o Compose**: o caso que forçou o ADR 0007. */
    fun onDeviceSettings(deviceId: String): Route = Route.DeviceSettings(deviceId)

    /**
     * Deeplink vira **pilha**, não tela solta: abrir um device direto da notificação
     * precisa deixar a lista embaixo, senão o voltar joga o usuário para fora do app.
     *
     * Formato: `kmpfoundation://device/<id>`.
     *
     * @return `null` quando o link não é reconhecido — a casca ignora e segue com o
     * que já estava na tela, em vez de navegar para lugar nenhum.
     */
    fun fromDeeplink(uri: String): List<Route>? {
        val path = uri.substringAfter(DEEPLINK_SCHEME, missingDelimiterValue = "")
        if (path.isEmpty()) return null

        val segments = path.split("/").filter { it.isNotBlank() }
        return when {
            segments.size == 2 && segments[0] == "device" ->
                listOf(Route.Devices, Route.DeviceDetail(segments[1]))

            segments.size == 1 && segments[0] == "devices" ->
                listOf(Route.Devices)

            else -> null
        }
    }

    private const val DEEPLINK_SCHEME = "kmpfoundation://"
}
