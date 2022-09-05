package io.nexure.fsm

interface Action<N : Any> {
    fun action(signal: N)
}
