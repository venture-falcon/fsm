package io.nexure.fsm

interface Action<N : Any> {
    suspend fun action(signal: N)
}
