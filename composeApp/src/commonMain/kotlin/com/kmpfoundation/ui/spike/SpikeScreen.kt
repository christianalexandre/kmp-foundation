package com.kmpfoundation.ui.spike

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp

/**
 * Tela do spike do RFC 0001 — **não é tela de produto**. Existe para medir o custo
 * de manter N raízes Compose vivas no iOS, uma por destino, que é o que a opção 3
 * do RFC exige e que ninguém mediu ainda.
 *
 * O conteúdo é propositalmente **representativo, não mínimo**: uma tela em branco
 * subestimaria o custo, e o que o app alvo tem em volume é a cauda longa de telas
 * de configuração — lista rolável com texto e controles. Medir um `Box` vazio
 * responderia a pergunta errada.
 *
 * [onFirstDraw] dispara na primeira passagem de **desenho**, não de composição: é
 * o instante mais próximo de "o usuário viu a tela", que é o que a medição de
 * tempo até o primeiro frame precisa.
 */
@Composable
fun SpikeScreen(
    depth: Int,
    onFirstDraw: () -> Unit,
    onPushNext: () -> Unit,
    onPushNative: () -> Unit,
) {
    val reporter = remember { FirstDrawReporter() }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        reporter.reportOnce(onFirstDraw)
                    },
            ) {
                // Sem header com o número do destino: a nav bar do UIKit já o mostra,
                // e é ela que precisa estar em cena para a transição ser representativa.
                Button(
                    onClick = onPushNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = HEADER_PADDING, end = HEADER_PADDING, top = HEADER_PADDING),
                ) {
                    Text("Empilhar Compose ${depth + 1}")
                }
                // O destino nativo é o que valida a intercalação: dele sai outro
                // destino Compose, formando o sanduíche Compose → nativo → Compose
                // que é o requisito que forçou a opção 3 do RFC.
                OutlinedButton(
                    onClick = onPushNative,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HEADER_PADDING),
                ) {
                    Text("Empilhar NATIVO ${depth + 1}")
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rowsFor(depth)) { row ->
                        SpikeRow(row)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SpikeRow(row: SpikeRow) {
    Column(modifier = Modifier.fillMaxWidth().padding(ROW_PADDING)) {
        Text(text = row.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = row.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class SpikeRow(val title: String, val subtitle: String)

private fun rowsFor(depth: Int): List<SpikeRow> = List(ROWS_PER_SCREEN) { index ->
    SpikeRow(
        title = "Ajuste ${index + 1} do destino $depth",
        subtitle = "Descrição de apoio para dar ao texto um volume realista de layout",
    )
}

/**
 * Guarda o "já reportei" fora do estado do Compose de propósito: escrever num
 * `MutableState` durante a fase de desenho agendaria recomposição a cada frame —
 * exatamente o custo que a medição quer observar, e não causar.
 */
private class FirstDrawReporter {
    private var reported = false

    fun reportOnce(onFirstDraw: () -> Unit) {
        if (reported) return
        reported = true
        onFirstDraw()
    }
}

private const val ROWS_PER_SCREEN = 40
private val HEADER_PADDING = 16.dp
private val ROW_PADDING = 12.dp
