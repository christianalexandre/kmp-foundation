package com.kmpfoundation.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A rota atravessa o extra do `Intent`, e é isso que faz a navegação sobreviver à
 * morte de processo no Android sem código nosso. Se a serialização quebrar, o
 * sintoma é o app restaurar no lugar errado — difícil de atribuir à causa depois.
 */
class RouteCodecTest {

    @Test
    fun routesSurviveARoundTrip() {
        val routes = listOf(
            Route.Welcome,
            Route.Devices,
            Route.DeviceDetail("cam-1"),
            Route.DeviceSettings("cam-1"),
        )

        routes.forEach { route ->
            assertEquals(route, RouteCodec.decode(RouteCodec.encode(route)))
        }
    }

    @Test
    fun brokenTextDecodesToNullInsteadOfCrashing() {
        // O caso real: Intent gravado por uma versão antiga do app, restaurado
        // depois da atualização. A casca cai na rota inicial em vez de crashar.
        assertNull(RouteCodec.decodeOrNull("{\"type\":\"Inexistente\"}"))
        assertNull(RouteCodec.decodeOrNull("nada disso é json"))
        assertNull(RouteCodec.decodeOrNull(null))
    }
}
