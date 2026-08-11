package com.kmpfoundation.core.platform

/**
 * Fake of the native engine: lets `commonTest` cover every platform-dependent
 * path without a device or a simulator.
 */
class FakeGreetingEngine(
    override val isAvailable: Boolean = true,
    private val greeting: String? = "hello from fake",
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
