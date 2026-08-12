package com.kmpfoundation.core.result

/** Resultado tipado de uma operação que pode falhar — ADR 0002. */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

/** Fechado por comportamento de UI, não por causa técnica — ADR 0002. */
sealed interface AppError {
    /** Sem conexão ou servidor inacessível: cabe oferecer "tentar de novo". */
    data object Connectivity : AppError

    /** O recurso não existe (404 e equivalentes): tentar de novo não resolve. */
    data object NotFound : AppError

    /** Sessão expirada ou ausente: leva para o fluxo de autenticação. */
    data object Unauthorized : AppError

    /** Plataforma indisponível/negada (permissão, hardware, SDK nativo). */
    data class Platform(val reason: String) : AppError

    /** O que não se encaixa acima. `cause` só para log — nunca para a tela. */
    data class Unexpected(val cause: Throwable? = null) : AppError
}
