package com.kmpfoundation.core.platform

import com.kmpfoundation.core.result.AppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceScannerTest {

    @Test
    fun emitsEachDeviceTheScannerFinds() = runTest {
        val scanner = FakeDeviceScanner()
        val events = mutableListOf<ScanEvent>()

        val job = launch { scanner.scan().collect { events += it } }
        advanceUntilIdle()
        scanner.emit("a", "Câmera A")
        scanner.emit("b", "Câmera B")
        scanner.finish()
        advanceUntilIdle()
        job.join()

        assertEquals(
            listOf<ScanEvent>(
                ScanEvent.Found(DiscoveredDevice("a", "Câmera A")),
                ScanEvent.Found(DiscoveredDevice("b", "Câmera B")),
            ),
            events,
        )
    }

    @Test
    fun scanStartsOnlyWhenSomeoneCollects() = runTest {
        val scanner = FakeDeviceScanner()

        scanner.scan()
        advanceUntilIdle()

        assertEquals(0, scanner.startCount, "fluxo frio não deve iniciar sem coletor")
    }

    // O teste que justifica todo o Cancellable: abandonar a coleta tem de
    // desligar o recurso nativo, senão o rádio segue ligado em background.
    @Test
    fun cancellingCollectionStopsTheNativeScan() = runTest {
        val scanner = FakeDeviceScanner()

        val job = launch { scanner.scan().collect { } }
        advanceUntilIdle()
        assertTrue(scanner.isScanning, "a busca deveria estar em andamento")

        job.cancel()
        advanceUntilIdle()

        assertEquals(1, scanner.cancelCount)
        assertFalse(scanner.isScanning, "a busca nativa deveria ter parado")
    }

    @Test
    fun failureFromThePlatformBecomesTypedError() = runTest {
        val scanner = FakeDeviceScanner()
        val events = mutableListOf<ScanEvent>()

        val job = launch { scanner.scan().collect { events += it } }
        advanceUntilIdle()
        scanner.finish(errorMessage = "bluetooth desligado")
        advanceUntilIdle()
        job.join()

        assertEquals(listOf<ScanEvent>(ScanEvent.Failed(AppError.Platform("bluetooth desligado"))), events)
    }

    @Test
    fun unavailableScannerFailsWithoutStarting() = runTest {
        val scanner = FakeDeviceScanner(isAvailable = false)
        val events = mutableListOf<ScanEvent>()

        scanner.scan().collect { events += it }

        assertEquals(1, events.size)
        assertTrue(events.single() is ScanEvent.Failed)
        assertEquals(0, scanner.startCount)
    }
}
