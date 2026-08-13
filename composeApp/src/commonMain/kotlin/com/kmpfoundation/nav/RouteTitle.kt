package com.kmpfoundation.nav

/**
 * O título de cada destino, em **um lugar só**.
 *
 * Antes disto o título existia apenas no iOS, dentro da casca: o Android não tinha
 * nenhum e ninguém percebia, porque não havia onde comparar. Título é texto de
 * produto — se cada casca escreve o seu, as duas plataformas divergem na primeira
 * alteração, exatamente como aconteceria com a política de navegação (ADR 0007).
 *
 * Quem desenha o cabeçalho continua sendo cada plataforma — Compose nos destinos
 * Compose, `View`/SwiftUI nos nativos —, mas o texto todos leem daqui.
 *
 * A **raiz não usa isto**: por decisão de produto, a tela inicial não tem
 * cabeçalho. O título só aparece quando há de onde voltar.
 */
fun titleOf(route: Route): String = when (route) {
    is Route.Devices -> "Dispositivos"

    // O id como título é o que a rota carrega — ADR 0007: rota leva id, não objeto.
    // Um app real trocaria isto pelo nome, que ele busca no repositório.
    is Route.DeviceDetail -> route.deviceId

    is Route.DeviceSettings -> "Ajustes"
}
