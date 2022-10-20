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
     * Execute a transition from state [current] to another state depending on event [event].
     * If an action is associated with the state transition, it will then be executed,
     * with [signal] as input. Returns a [Transition] indicating if the transition was permitted and
     * successful or not.
     *
     * Note that any [Exception] that is thrown will be caught and a part of the Transition
     * return value.
     */
    fun onEvent(current: S, event: E, signal: N): Transition<S>

    companion object {
        fun <S : Any, E : Any, N : Any> builder(): StateMachineBuilder<S, E, N> = StateMachineBuilder()
    }
}
