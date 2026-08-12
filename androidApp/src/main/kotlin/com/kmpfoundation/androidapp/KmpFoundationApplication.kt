package com.kmpfoundation.androidapp

import android.app.Application
import com.kmpfoundation.core.platform.AndroidGreetingEngine
import com.kmpfoundation.di.AppGraph

/** Dono do composition root no Android — ADR 0003. */
class KmpFoundationApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(AndroidGreetingEngine()) }
}
