package com.kmpfoundation.core.platform

import android.os.Handler
import android.os.Looper

/**
 * No Android a engine é Kotlin puro; o valor do exemplo está em provar que o
 * mesmo contrato serve para os dois lados (no iOS quem implementa é o Swift).
 *
 * Repare que a engine **não guarda estado de chamada**: cada `greet` devolve o
 * próprio handle, e é ele que segura o `Runnable`. Sem isso, duas chamadas
 * concorrentes na mesma engine cancelariam uma à outra (ADR 0004).
 */
class AndroidGreetingEngine : GreetingEngine {
    private val handler = Handler(Looper.getMainLooper())

    override val isAvailable: Boolean = true

    override fun greet(name: String, callback: GreetingCallback): Cancellable {
        val work = Runnable { callback.onResult("Olá, $name — de ${platformName()}", null) }
        handler.post(work)
        return object : Cancellable {
            override fun cancel() {
                handler.removeCallbacks(work)
            }
        }
    }
}
