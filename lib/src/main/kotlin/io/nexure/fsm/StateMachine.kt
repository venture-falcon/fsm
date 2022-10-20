package io.nexure.fsm

/**
 * S = State
 * E = Event triggering a transition between two states
 * N = Signal, data associated with a state change
 */
interface StateMachine<S : Any, E : Any, N : Any> {
    /**
     * Return all the possible states of the state machine
     */
    fun states(): Set<S>

    /**
     * Return the start (initial) state of the state machine
     */
    fun initialState(): S

    /**
     * Return all the terminal states of the state machine
     */
    fun terminalStates(): Set<S>

    /**
     * Execute a transition into the _initial_ state. If an action is associated with entering the
     * initial state, it will then be executed, with [signal] as input.
     *
     * Returns the new (initial) state
     */
    fun onInitial(signal: N): S

    /**
     * Execute a transition from state [current] to another state depending on event [event].
     * If an action is associated with the state transition, it will then be executed,
     * with [signal] as input. Returns the new state if the transition was successful.
     *
     * @throws IllegalTransitionException if not valid transition exists from state [current]
     * with event [event].
     */
    @Throws(IllegalTransitionException::class)
    fun onEvent(current: S, event: E, signal: N): S

    companion object {
        fun <S : Any, E : Any, N : Any> builder(): StateMachineBuilder<S, E, N> = StateMachineBuilder()
    }
}

class IllegalTransitionException(override val message: String?) : Exception() {
    companion object {
        fun <S> forInitialState(target: S): IllegalTransitionException =
            IllegalTransitionException("Illegal state transition into non initial state as initial: '$target'")

        fun <S, E> forState(source: S?, target: S?, event: E): IllegalTransitionException =
            IllegalTransitionException("Illegal state transition from '$source' to '$target' with event '$event'")

        fun <S, E> forEvent(source: S?, event: E?): IllegalTransitionException =
            IllegalTransitionException("Illegal event ($event) for state '$source'")
    }
}
