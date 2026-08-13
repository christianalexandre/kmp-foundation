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
    val start: Route = Route.Devices

    /** Vai para uma tela **nativa** — a casca descobre isso por [Route.isNative]. */
    fun onDeviceSelected(deviceId: String): Route = Route.DeviceDetail(deviceId)

    /**
     * Ajustes de um dispositivo. Chamada de **dois lugares**: da lista, e é o salto
     * Compose → Compose; e da tela nativa, e é o Compose voltando por cima dela —
     * o caso que forçou o ADR 0007. A função é a mesma porque a decisão é a mesma.
     */
    fun onDeviceSettings(deviceId: String): Route = Route.DeviceSettings(deviceId)
}
