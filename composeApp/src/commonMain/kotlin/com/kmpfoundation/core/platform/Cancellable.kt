package com.kmpfoundation.core.platform

/**
 * Handle de UMA chamada nativa em andamento. Cancelá-lo não pode afetar
 * nenhuma outra chamada da mesma engine — ADR 0004.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Implementação pronta para as engines não precisarem declarar uma classe só
 * para embrulhar o cancelamento. Existe sobretudo pelo lado Swift: um protocolo
 * Objective-C não aceita closure, então sem isto cada engine iOS carregaria uma
 * classe de nove linhas. Com ela, `return CallHandle(onCancel: { ... })`.
 */
class CallHandle(private val onCancel: () -> Unit) : Cancellable {
    override fun cancel() {
        onCancel()
    }
}

/** Handle de operação que não tem o que cancelar (resposta já em memória). */
val NoOpCancellable: Cancellable = CallHandle { }
