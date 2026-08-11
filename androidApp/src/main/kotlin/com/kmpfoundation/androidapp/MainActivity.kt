package com.kmpfoundation.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kmpfoundation.core.platform.AndroidGreetingEngine
import com.kmpfoundation.core.ui.App
import com.kmpfoundation.di.AppGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = AppGraph(AndroidGreetingEngine())
        setContent {
            App(graph)
        }
    }
}
