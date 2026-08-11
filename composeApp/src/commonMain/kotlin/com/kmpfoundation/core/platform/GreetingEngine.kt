package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * MODELO da fronteira com engines nativas assíncronas (câmera, BLE, player,
 * SDK de terceiro). O Swift implementa esta interface diretamente, então a
 * assinatura obedece ao que o header Objective-C consegue exportar:
 *
 * - **sem `suspend`** — vira bloco de completion ilegível no Swift;
 * - **sem `sealed`/genérico** nos parâmetros — não sobrevive à exportação;
 * - callback explícito, com `cancel()` para o cancelamento cooperativo.
 *
 * O Kotlin nunca chama isto na mão: usa o adapter [greet], que devolve o tipo
 * selado e liga o cancelamento da coroutine ao `cancel()` da engine.
 */
interface GreetingEngine {
    val isAvailable: Boolean

    fun greet(name: String, callback: GreetingCallback)

    fun cancel()
}

/** Callback plano: Swift conforma sem ponte de tipos. */
interface GreetingCallback {
    fun onResult(greeting: String?, errorMessage: String?)
}

/**
 * Adapter para o lado Kotlin: converte o callback em `suspend` e propaga o
 * cancelamento da coroutine para a engine nativa.
 */
suspend fun GreetingEngine.greet(name: String): Outcome<String> {
    if (!isAvailable) {
        return Outcome.Failure(AppError.Platform("engine indisponível nesta plataforma"))
    }
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        greet(
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
    }
}
