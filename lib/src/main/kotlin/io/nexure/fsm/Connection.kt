package io.nexure.fsm

internal sealed class Connection<S : Any, E : Any, N : Any> : Edge<S, E> {
    /**
     * A connection for an initial state, i.e. when transitioning into a _start_ state in the
     * state machine.
     */
    class Initial<S : Any, E : Any, N : Any>(
        val target: S,
        val action: (N) -> Unit
    ) : Connection<S, E, N>()

    /**
     * A connection for a transition state from one state to another, with an event.
     */
    class Transition<S : Any, E : Any, N : Any>(
        val source: S,
        val target: S,
        val event: E,
        val action: (N) -> Unit
    ) : Connection<S, E, N>()

    override fun source(): S? {
        return when (this) {
            is Initial -> null
            is Transition -> source
        }
    }

    override fun target(): S {
        return when (this) {
            is Initial -> target
            is Transition -> target
        }
    }

    override fun event(): E? {
        return when (this) {
            is Initial -> null
            is Transition -> event
        }
    }
}
