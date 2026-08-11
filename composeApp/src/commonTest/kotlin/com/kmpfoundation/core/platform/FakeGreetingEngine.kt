package com.kmpfoundation.core.platform

/**
 * Fake da engine nativa: é o que permite testar em `commonTest` toda a lógica
 * que depende de plataforma, sem device e sem simulador.
 */
class FakeGreetingEngine(
    override val isAvailable: Boolean = true,
    private val greeting: String? = "olá do fake",
    private val errorMessage: String? = null,
    private val respondImmediately: Boolean = true,
) : GreetingEngine {
    var cancelCount = 0
        private set
    var lastName: String? = null
        private set

    private var pending: GreetingCallback? = null

    override fun greet(name: String, callback: GreetingCallback) {
        lastName = name
        if (respondImmediately) {
            callback.onResult(greeting, errorMessage)
        } else {
            pending = callback
        }
    }

    override fun cancel() {
        cancelCount++
        pending = null
    }
}
