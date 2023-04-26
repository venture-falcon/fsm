package io.nexure.fsm

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

internal fun <T> Deferred<T>.awaitBlocking() = runBlocking { this@awaitBlocking.await() }
