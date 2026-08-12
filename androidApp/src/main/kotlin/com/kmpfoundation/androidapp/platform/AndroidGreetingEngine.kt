package com.kmpfoundation.androidapp.platform

import android.os.Handler
import android.os.Looper
import com.kmpfoundation.core.platform.Cancellable
import com.kmpfoundation.core.platform.GreetingCallback
import com.kmpfoundation.core.platform.GreetingEngine

class AndroidGreetingEngine : GreetingEngine {
    private val handler = Handler(Looper.getMainLooper())

    override val isAvailable: Boolean = true

    override fun greet(name: String, callback: GreetingCallback): Cancellable {
        val work = Runnable { callback.onResult("Olá, $name", null) }
        handler.post(work)
        // O handle retém o Runnable; a engine não guarda estado de chamada (ADR 0004).
        return object : Cancellable {
            override fun cancel() {
                handler.removeCallbacks(work)
            }
        }
    }
}
