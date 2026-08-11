package com.kmpfoundation.core.platform

import android.os.Handler
import android.os.Looper

/**
 * No Android a engine é Kotlin puro; o valor do exemplo está em provar que o
 * mesmo contrato serve para os dois lados (no iOS quem implementa é o Swift).
 */
class AndroidGreetingEngine : GreetingEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    override val isAvailable: Boolean = true

    override fun greet(name: String, callback: GreetingCallback) {
        val work = Runnable { callback.onResult("Olá, $name — de ${platformName()}", null) }
        pending = work
        handler.post(work)
    }

    override fun cancel() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }
}
