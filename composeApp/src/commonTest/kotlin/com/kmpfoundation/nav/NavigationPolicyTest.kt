package com.kmpfoundation.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A política é o que torna a regra "a casca não decide destino" verificável: se
 * estes testes passam, a decisão está no comum e não duplicada nas duas cascas.
 */
class NavigationPolicyTest {

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

    @Test
    fun deeplinkToADeviceKeepsTheListUnderneath() {
        val routes = NavigationPolicy.fromDeeplink("kmpfoundation://device/cam-7")

        // Duas rotas, não uma: abrir o device direto da notificação sem a lista
        // embaixo faria o voltar jogar o usuário para fora do app.
        assertEquals(listOf(Route.Devices, Route.DeviceDetail("cam-7")), routes)
    }

    @Test
    fun deeplinkToTheListIsASingleRoute() {
        assertEquals(listOf(Route.Devices), NavigationPolicy.fromDeeplink("kmpfoundation://devices"))
    }

    @Test
    fun unknownDeeplinkNavigatesNowhere() {
        assertNull(NavigationPolicy.fromDeeplink("kmpfoundation://cameras/1/live"))
        assertNull(NavigationPolicy.fromDeeplink("https://exemplo.com/device/1"))
        assertNull(NavigationPolicy.fromDeeplink("kmpfoundation://"))
    }
}
