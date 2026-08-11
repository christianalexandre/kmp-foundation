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
    val calls = mutableListOf<Call>()

    override fun greet(name: String, callback: GreetingCallback): Cancellable {
        val call = Call(name, callback)
        calls += call
        if (respondImmediately) {
            callback.onResult(greeting, errorMessage)
        }
        return call
    }

    /** Responds to a pending call, so tests can interleave concurrent calls. */
    fun respondTo(call: Call, greeting: String? = null, errorMessage: String? = null) {
        call.callback.onResult(greeting, errorMessage)
    }

    class Call(val name: String, internal val callback: GreetingCallback) : Cancellable {
        var isCancelled = false
            private set

        override fun cancel() {
            isCancelled = true
        }
    }
}
