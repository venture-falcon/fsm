package io.nexure.fsm

internal interface Edge<S : Any, E : Any> {
    fun source(): S?
    fun target(): S
    fun event(): E?

    operator fun component1(): S? = source()
    operator fun component2(): S? = target()
    operator fun component3(): E? = event()
}
