package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Fronteira com engines nativas assíncronas (câmera, BLE, player, SDK de
 * terceiro). Formato e restrições de assinatura no SDD 02; o handle por
 * chamada, no ADR 0004.
 */
interface GreetingEngine {
    val isAvailable: Boolean

    fun greet(name: String, callback: GreetingCallback): Cancellable
}

/** Handle de UMA chamada em andamento. */
interface Cancellable {
    fun cancel()
}

interface GreetingCallback {
    fun onResult(greeting: String?, errorMessage: String?)
}

/** Adapter: o lado Kotlin nunca chama a engine direto. */
suspend fun GreetingEngine.greet(name: String): Outcome<String> {
    if (!isAvailable) {
        return Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma"))
    }
    return suspendCancellableCoroutine { continuation ->
        val handle = greet(
            name,
            object : GreetingCallback {
                override fun onResult(greeting: String?, errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resume(
                            when {
                                greeting != null -> Outcome.Success(greeting)

                                else -> Outcome.Failure(
                                    AppError.Platform(errorMessage ?: "falha desconhecida"),
                                )
                            },
                        )
                    }
                }
            },
        )
        // Só depois de ter o handle: cancelamento ocorrido nesse meio-tempo
        // dispara aqui mesmo, e nenhuma chamada nativa fica órfã.
        continuation.invokeOnCancellation { handle.cancel() }
    }
}
