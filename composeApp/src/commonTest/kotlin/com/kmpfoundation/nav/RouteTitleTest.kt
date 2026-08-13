package com.kmpfoundation.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O título ficou em `commonMain` justamente para não voltar a existir só no iOS.
 * Estes testes são o que torna "as duas plataformas dizem a mesma coisa"
 * verificável sem abrir dois apps.
 */
class RouteTitleTest {

    @Test
    fun everyDestinationHasATitle() {
        val routes = listOf(
            Route.Devices,
            Route.DeviceDetail("cam-1"),
            Route.DeviceSettings("cam-1"),
        )

        routes.forEach { route ->
            assertTrue(titleOf(route).isNotBlank(), "sem título: $route")
        }
    }

    @Test
    fun theNativeDestinationIsTitledByTheIdTheRouteCarries() {
        // A rota leva id, não objeto (ADR 0007) — então é o id que o cabeçalho tem
        // para mostrar. Um app real trocaria pelo nome buscado no repositório.
        assertEquals("cam-1", titleOf(Route.DeviceDetail("cam-1")))
    }
}
