package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome

/**
 * Caso **síncrono** da fronteira nativa: o dado só existe do lado da plataforma
 * (SDK, keychain, capability), mas a resposta vem na hora — sem callback, sem
 * ciclo de vida, sem nada para cancelar.
 *
 * Os outros dois casos, no SDD 03:
 * - o Kotlin/Native alcança a API sozinho → `expect/actual` ([platformName]);
 * - o trabalho demora e segura recurso → [DeviceScanner].
 */
interface GreetingEngine {
    val isAvailable: Boolean

    fun greeting(name: String): String
}

/** Adapter: o `isAvailable` vira falha tipada em vez de responsabilidade do chamador. */
fun GreetingEngine.greet(name: String): Outcome<String> =
    if (isAvailable) {
        Outcome.Success(greeting(name))
    } else {
        Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma"))
    }
