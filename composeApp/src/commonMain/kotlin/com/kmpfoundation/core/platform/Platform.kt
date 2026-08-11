package com.kmpfoundation.core.platform

/**
 * Caso simples da fronteira: dado de plataforma que não precisa de callback nem
 * de ciclo de vida. Para engines assíncronas, ver [GreetingEngine].
 */
expect fun platformName(): String
