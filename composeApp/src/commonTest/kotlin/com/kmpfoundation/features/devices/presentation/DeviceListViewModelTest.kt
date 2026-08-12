package com.kmpfoundation.features.devices.presentation

import com.kmpfoundation.core.platform.FakeDeviceScanner
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.features.devices.FakeDeviceRepository
import com.kmpfoundation.features.devices.data.PlatformDeviceRepository
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.domain.DiscoveryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scanner = FakeDeviceScanner()
    private val viewModel = DeviceListViewModel(PlatformDeviceRepository(scanner))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsIdleWithoutScanning() {
        assertEquals(DeviceListUiState(), viewModel.state.value)
        assertEquals(0, scanner.startCount)
    }

    @Test
    fun accumulatesDevicesAsTheyAreFound() = runTest(dispatcher) {
        viewModel.startScan()
        advanceUntilIdle()
        scanner.emit("a", "Câmera A")
        scanner.emit("b", "Câmera B")
        advanceUntilIdle()

        assertEquals(
            listOf(Device("a", "Câmera A"), Device("b", "Câmera B")),
            viewModel.state.value.devices,
        )
        assertTrue(viewModel.state.value.isScanning)
    }

    @Test
    fun stopScanTurnsOffTheNativeScan() = runTest(dispatcher) {
        viewModel.startScan()
        advanceUntilIdle()

        viewModel.stopScan()
        advanceUntilIdle()

        assertEquals(1, scanner.cancelCount)
        assertFalse(viewModel.state.value.isScanning)
    }

    @Test
    fun scanningTwiceInARowIsIgnoredWhileTheFirstRuns() = runTest(dispatcher) {
        viewModel.startScan()
        viewModel.startScan()
        advanceUntilIdle()

        assertEquals(1, scanner.startCount)
    }

    @Test
    fun platformFailureBecomesAMessageOnScreen() = runTest(dispatcher) {
        viewModel.startScan()
        advanceUntilIdle()
        scanner.finish(errorMessage = "bluetooth desligado")
        advanceUntilIdle()

        assertEquals("Busca indisponível neste aparelho.", viewModel.state.value.errorMessage)
    }

    @Test
    fun eachDomainErrorHasItsOwnUiMessage() = runTest(dispatcher) {
        val errors = listOf(
            AppError.Connectivity,
            AppError.NotFound,
            AppError.Unauthorized,
            AppError.Platform("x"),
            AppError.Unexpected(),
        )

        val messages = errors.map { error ->
            val model = DeviceListViewModel(
                FakeDeviceRepository(listOf(DiscoveryEvent.Failed(error))),
            )
            model.startScan()
            advanceUntilIdle()
            model.state.value.errorMessage
        }

        assertEquals(messages.size, messages.distinct().size, "duplicate messages: $messages")
    }
}
