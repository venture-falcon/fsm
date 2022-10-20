package io.nexure.fsm

/**
 * A connection for a transition state from one state to another, with an event.
 */
internal class Edge<S : Any, E : Any, N : Any>(
    val source: S,
    val target: S,
    val event: E,
    val action: (N) -> Unit
) {
    operator fun component1(): S = source
    operator fun component2(): S = target
    operator fun component3(): E = event

    override fun toString(): String = "$source -> $event -> $target"
}
