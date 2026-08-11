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
 * - callback explícito, com um [Cancellable] **por chamada** — ver ADR 0004.
 *
 * O Kotlin nunca chama isto na mão: usa o adapter [greet], que devolve o tipo
 * selado e liga o cancelamento da coroutine ao handle daquela chamada.
 */
interface GreetingEngine {
    val isAvailable: Boolean

    fun greet(name: String, callback: GreetingCallback): Cancellable
}

/**
 * Handle de UMA chamada em andamento. É a engine que o cria, e cancelá-lo não
 * pode afetar nenhuma outra chamada da mesma engine (ADR 0004).
 */
interface Cancellable {
    fun cancel()
}

/** Callback plano: Swift conforma sem ponte de tipos. */
interface GreetingCallback {
    fun onResult(greeting: String?, errorMessage: String?)
}

/**
 * Adapter para o lado Kotlin: converte o callback em `suspend` e propaga o
 * cancelamento da coroutine para a chamada nativa correspondente.
 */
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
        // Registrado DEPOIS de ter o handle: se a coroutine já tiver sido
        // cancelada nesse meio-tempo, o próprio invokeOnCancellation dispara na
        // hora — nenhuma chamada nativa fica órfã.
        continuation.invokeOnCancellation { handle.cancel() }
    }
}
