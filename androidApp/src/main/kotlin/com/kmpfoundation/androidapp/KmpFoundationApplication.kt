package com.kmpfoundation.androidapp

import android.app.Application
import com.kmpfoundation.core.platform.AndroidGreetingEngine
import com.kmpfoundation.di.AppGraph

/**
 * Dono do composition root no Android — ver ADR 0003. O grafo vive no processo,
 * não na Activity: criá-lo em `onCreate` reconstruiria tudo o que ele segura
 * (cliente HTTP, banco, SDKs nativos) a cada rotação de tela.
 */
class KmpFoundationApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(AndroidGreetingEngine()) }
}
