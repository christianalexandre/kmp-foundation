package com.kmpfoundation.features.welcome

import com.kmpfoundation.core.result.AppError
import com.kmpfoundation.core.result.Outcome
import com.kmpfoundation.features.welcome.domain.MessageRepository
import com.kmpfoundation.features.welcome.domain.WelcomeMessage

class FakeMessageRepository(
    private val outcome: Outcome<WelcomeMessage> = Outcome.Success(WelcomeMessage("hello from tests")),
) : MessageRepository {
    var callCount = 0
        private set

    override suspend fun welcomeMessage(): Outcome<WelcomeMessage> {
        callCount++
        return outcome
    }

    companion object {
        fun failing(error: AppError) = FakeMessageRepository(Outcome.Failure(error))
    }
}
