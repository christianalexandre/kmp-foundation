package com.kmpfoundation.di

import com.kmpfoundation.core.platform.FakeDeviceScanner
import com.kmpfoundation.features.devices.domain.Device
import com.kmpfoundation.features.devices.presentation.DeviceSettingsUiState
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun wiresTheEngineAllTheWayDownToTheScreenState() = runTest(dispatcher) {
        val scanner = FakeDeviceScanner()
        val graph = AppGraph(scanner)

        val viewModel = graph.devices.listViewModel()
        viewModel.startScan()
        advanceUntilIdle()
        scanner.emit("cam-1", "Câmera da sala")
        advanceUntilIdle()

        assertEquals(listOf(Device("cam-1", "Câmera da sala")), viewModel.state.value.devices)
    }

    /**
     * O que este teste protege é a consequência que o ADR 0007 registra como
     * obrigatória: com uma raiz Compose por destino, a tela de ajustes abre numa
     * composição nova e vazia. Só chega ao nome do dispositivo porque o grafo — e o
     * repositório dentro dele — vive no escopo de aplicação (ADR 0003).
     *
     * Se alguém mover o repositório para dentro de uma tela, este teste cai.
     */
    @Test
    fun theSecondScreenSeesWhatTheFirstOneFound() = runTest(dispatcher) {
        val scanner = FakeDeviceScanner()
        val graph = AppGraph(scanner)

        val list = graph.devices.listViewModel()
        list.startScan()
        advanceUntilIdle()
        scanner.emit("cam-1", "Câmera da sala")
        advanceUntilIdle()

        // Outra tela, outro ViewModel, outra raiz Compose — só o id atravessou.
        val settings = graph.devices.settingsViewModel("cam-1")
        advanceUntilIdle()

        assertEquals(
            DeviceSettingsUiState.Content(Device("cam-1", "Câmera da sala")),
            settings.state.value,
        )
    }
}
