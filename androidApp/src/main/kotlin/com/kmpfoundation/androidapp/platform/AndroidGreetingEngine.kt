package com.kmpfoundation.androidapp.platform

import com.kmpfoundation.core.platform.GreetingEngine

/** Caso síncrono: responde na hora, não há recurso a desligar. */
class AndroidGreetingEngine : GreetingEngine {
    override val isAvailable: Boolean = true

    override fun greeting(name: String): String = "Olá, $name"
}
