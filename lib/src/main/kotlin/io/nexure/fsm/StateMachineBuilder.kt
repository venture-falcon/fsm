package io.nexure.fsm

@Suppress("UNUSED_PARAMETER")
private fun <N : Any> noOp(signal: N) {}

/**
 * - [S] - the type of state that the state machine handles
 * - [E] - the type of events that the can trigger state changes
 * - [N] - the type of the input used in actions which are executed on state transitions
 */
class StateMachineBuilder<S : Any, E : Any, N : Any> private constructor(
    private var initialState: S? = null,
    private val transitions: List<Edge<S, E, N>> = emptyList(),
    private val interceptors: List<(S, S, E, N) -> (N)> = emptyList(),
    private val postInterceptors: List<(S, S, E, N) -> Unit> = emptyList()
) {
    constructor() : this(null, emptyList(), emptyList(), emptyList())

    /**
     * Set the initial state for this state machine. There must be exactly one initial state,
     * no more or less. Failing to set an initial state for a state machine will cause an
     * [InvalidStateMachineException] to be thrown when [build()] is invoked.
     *
     * Calling this method more than once, with a different initial state will also cause an
     * [InvalidStateMachineException] to be thrown, but immediately upon the second call to this
     * method rather when the state machine is built.
     */
    @Throws(InvalidStateMachineException::class)
    fun initial(state: S): StateMachineBuilder<S, E, N> {
        return if (initialState == null) {
            StateMachineBuilder(state, transitions, interceptors, postInterceptors)
        } else if (state === initialState) {
            StateMachineBuilder(initialState, transitions, interceptors, postInterceptors)
        } else {
            throw InvalidStateMachineException("There can only be one initial state")
        }
    }

    /**
     * Create a state transition from [source] state to [target] state that will be triggered by
     * [event], and execute an optional [action] when doing the state transition. There can be
     * multiple events that connect [source] and [target], but there must never be any ambiguous
     * transitions.
     *
     * For example, having both of the following transitions, would NOT be permitted
     * - `(S1, E1) -> S2`
     * - `(S1, E1) -> S3`
     *
     * since it would not be clear if the new state should be `S2` or `S3` when event `E1` is
     * received.
     */
    fun connect(
        source: S,
        target: S,
        event: E,
        action: (signal: N) -> Unit = ::noOp
    ): StateMachineBuilder<S, E, N> = connect(Edge(source, target, event, action))

    fun connect(source: S, target: S, event: E, action: Action<N>): StateMachineBuilder<S, E, N> =
        connect(source, target, event, action::action)

    private fun connect(edge: Edge<S, E, N>): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions.plus(edge), interceptors, postInterceptors)

    /**
     * Add an interceptor that is run _before_ any state machine action or state transition is done.
     * The interceptor will only be run if the current state permits the event in question. No
     * execution of the interceptor will be done if the state is rejected.
     */
    fun intercept(
        interception: (source: S, target: S, event: E, signal: N) -> N
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions, interceptors.plus(interception), postInterceptors)

    /**
     * Add an interceptor that is run _after_ a successful processing of an event by the state
     * machine. This interceptor will not be run if the event was rejected by the state machine, or
     * if there was an exception thrown while executing the state machine action (if any).
     */
    fun postIntercept(
        interception: (source: S, target: S, event: E, signal: N) -> Unit
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions, interceptors, postInterceptors.plus(interception))

    /**
     * @throws InvalidStateMachineException if the configured state machine is not valid. The main
     * reasons for a state machine not being valid are:
     * - No initial state
     * - More than one initial state
     * - The state machine is not connected (some states are not possible to reach from the initial
     * state)
     * - The same source state and event is defined twice
     */
    @Throws(InvalidStateMachineException::class)
    fun build(): StateMachine<S, E, N> {
        val initState: S = initialState
            ?: throw InvalidStateMachineException("No initial state set for state machine")

        StateMachineValidator.validate(initState, transitions)

        return StateMachineImpl(
            initState,
            transitions,
            interceptors,
            postInterceptors
        )
    }
}
