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

    companion object {
        fun <S : Any, E : Any> builder(): StateMachineBuilder.Uninitialized<S, E> = StateMachineBuilder()
    }
}

/**
 * Execute a transition from [state] to another state depending on [event].
 * Returns a [Transition] indicating if the transition was permitted and
 * successful or not by the state machine.
 *
 * It is recommended that the return value is checked for the desired outcome, if it is critical
 * that an event for example is accepted and not rejected.
 *
 * This extension method is just syntactic sugar for calling
 * ```kotlin
 * stateMachine.onEvent(currentState, event).onTransition { newState ->
 *     // Do something
 * }
 * ```
 */
inline fun <S : Any, E : Any> StateMachine<S, E>.onEvent(
    state: S,
    event: E,
    action: (state: S) -> Unit
): Transition<S> = onEvent(state, event).onTransition { action(it) }

