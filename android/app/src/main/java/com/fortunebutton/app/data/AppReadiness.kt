package com.fortunebutton.app.data

import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReadiness @Inject constructor() {
    private val ready = CompletableDeferred<Unit>()

    suspend fun awaitReady() {
        ready.await()
    }

    fun markReady() {
        if (!ready.isCompleted) ready.complete(Unit)
    }
}
