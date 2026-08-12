package com.kmpfoundation.core.platform

class FakeGreetingEngine(
    override val isAvailable: Boolean = true,
    private val greeting: String = "hello from fake",
) : GreetingEngine {
    var lastName: String? = null
        private set

    override fun greeting(name: String): String {
        lastName = name
        return greeting
    }
}
