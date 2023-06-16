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

    /**
     * Execute a transition from [state] to another state depending on [event].
     * Returns a [Transition] indicating if the transition was permitted and
     * successful or not by the state machine.
     *
     * It is recommended that the return value is checked for the desired outcome, if it is critical
     * that an event for example is accepted and not rejected.
     */
    fun onEvent(state: S, event: E): Transition<S>

    /**
     * Return a list of events that are accepted by the state machine in the given state. The returned list will be an
     * empty list if the state is a terminal state.
     */
    fun acceptedEvents(state: S): List<E>

    companion object {
        fun <S : Any, E : Any> builder(): StateMachineBuilder.Uninitialized<S, E> = StateMachineBuilder()
    }
}
