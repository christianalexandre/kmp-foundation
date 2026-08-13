package com.kmpfoundation.features.devices.presentation

import com.kmpfoundation.core.platform.FakeDeviceScanner
import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.devices.FakeDeviceRepository
import com.kmpfoundation.features.devices.data.PlatformDeviceRepository
import com.kmpfoundation.features.devices.domain.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class DeviceSettingsViewModelTest {

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
    fun startsLoadingBeforeTheLookupAnswers() {
        val viewModel = DeviceSettingsViewModel("cam-1", FakeDeviceRepository())

        assertEquals(DeviceSettingsUiState.Loading, viewModel.state.value)
    }

    @Test
    fun resolvesTheDeviceTheScanFound() = runTest(dispatcher) {
        // Pelo repositório real, porque o que está sendo verificado é a memória
        // dele: a rota trouxe só o id, e o nome tem de vir da busca anterior.
        val scanner = FakeDeviceScanner()
        val repository = PlatformDeviceRepository(scanner)

        val scan = launch { repository.discover().collect { } }
        advanceUntilIdle()
        scanner.emit("cam-1", "Câmera da sala")
        advanceUntilIdle()
        scan.cancel()

        val viewModel = DeviceSettingsViewModel("cam-1", repository)
        advanceUntilIdle()

        assertEquals(
            DeviceSettingsUiState.Content(Device("cam-1", "Câmera da sala")),
            viewModel.state.value,
        )
    }

    /**
     * O caso real: morte de processo. O sistema restaura o `Intent` com a rota, mas
     * o repositório em memória voltou vazio — a tela precisa dizer isso em vez de
     * ficar carregando para sempre. É por aqui que se vê por que o ADR 0007 pede um
     * repositório durável quando o dado importa.
     */
    @Test
    fun unknownDeviceBecomesAMessageInsteadOfAnEmptyScreen() = runTest(dispatcher) {
        val viewModel = DeviceSettingsViewModel(
            "cam-1",
            PlatformDeviceRepository(FakeDeviceScanner()),
        )
        advanceUntilIdle()

        assertEquals(
            DeviceSettingsUiState.Error("Dispositivo não encontrado. Volte e busque de novo."),
            viewModel.state.value,
        )
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
            val viewModel = DeviceSettingsViewModel(
                "cam-1",
                FakeDeviceRepository(lookup = Outcome.Failure(error)),
            )
            advanceUntilIdle()
            (viewModel.state.value as DeviceSettingsUiState.Error).message
        }

        assertEquals(messages.size, messages.distinct().size, "duplicate messages: $messages")
    }
}
