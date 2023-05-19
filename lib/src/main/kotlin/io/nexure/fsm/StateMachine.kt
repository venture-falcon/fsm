package io.nexure.fsm

/**
 * S = State
 * E = Event triggering a transition between two states
 */
interface StateMachine<S : Any, E : Any> {
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
     * Infer the state from a list of events, in the order that they happened
     */
    fun reduceState(events: List<E>): S

//    /**
//     * Transition into the initial state, executing the action - if any - setup in the state machine for initialization.
//     *
//     * Returns the state after the initialization, which will always be the initial state.
//     */
//    suspend fun initialize(signal: N): S

    /**
     * Execute a transition from [state] to another state depending on [event].
     * If an action is associated with the state transition, it will then be executed,
     * with [signal] as input. Returns a [Transition] indicating if the transition was permitted and
     * successful or not.
     *
     * It is recommended that the return value is checked for the desired outcome, if it is critical
     * that an event for example is accepted and not rejected.
     */
    suspend fun onEvent(state: S, event: E, action: suspend () -> Unit = {}): Transition<S>

    companion object {
        fun <S : Any, E : Any, N : Any> builder(): StateMachineBuilder<S, E> = StateMachineBuilder()
    }
}
