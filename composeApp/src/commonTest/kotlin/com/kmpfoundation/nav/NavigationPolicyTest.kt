package com.kmpfoundation.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A política é o que torna a regra "a casca não decide destino" verificável: se
 * estes testes passam, a decisão está no comum e não duplicada nas duas cascas.
 */
class NavigationPolicyTest {

    @Test
    fun theAppStartsOnAComposeScreen() {
        // Se a rota inicial virar nativa sem querer, o app abre sem cabeçalho E sem
        // o Compose que o desenha — sintoma difícil de ligar à causa.
        assertEquals(Route.Devices, NavigationPolicy.start)
        assertTrue(!NavigationPolicy.start.isNative)
    }

    @Test
    fun selectingADeviceGoesToANativeDestination() {
        val route = NavigationPolicy.onDeviceSelected("cam-1")

        assertEquals(Route.DeviceDetail("cam-1"), route)
        // É por esta flag que a casca sabe montar um view controller nativo em vez
        // de uma raiz Compose. Se ela mudar sem querer, a tela abre em branco.
        assertTrue(route.isNative)
    }

    @Test
    fun leavingTheNativeScreenGoesBackToCompose() {
        val route = NavigationPolicy.onDeviceSettings("cam-1")

        assertEquals(Route.DeviceSettings("cam-1"), route)
        assertTrue(!route.isNative)
    }
}
