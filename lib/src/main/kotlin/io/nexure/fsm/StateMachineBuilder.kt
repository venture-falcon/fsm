package io.nexure.fsm

@Suppress("UNUSED_PARAMETER")
private fun <N : Any> noOp(signal: N) {}

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
     * [InvalidStateMachineException] to be thrown, but immedaitely upon the second call to this
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

    fun connect(
        current: S,
        next: S,
        event: E,
        action: (signal: N) -> Unit = ::noOp
    ): StateMachineBuilder<S, E, N> = connect(Edge(current, next, event, action))

    fun connect(current: S, next: S, event: E, action: Action<N>): StateMachineBuilder<S, E, N> =
        connect(current, next, event, action::action)

    private fun connect(edge: Edge<S, E, N>): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions.plus(edge), interceptors, postInterceptors)

    /**
     * Add an interceptor that is run _before_ any state machine action or state transition is done.
     * The interceptor will only be run if the current state permits the event in question. No
     * execution of the interceptor will be done if the state is rejected.
     */
    fun intercept(
        interception: (current: S, next: S, event: E, signal: N) -> N
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions, interceptors.plus(interception), postInterceptors)

    /**
     * Add an interceptor that is run _after_ a successful processing of an event by the state
     * machine. This interceptor will not be run if the event was rejected by the state machine, or
     * if there was an exception thrown while executing the state machine action (if any).
     */
    fun postIntercept(
        interception: (current: S, next: S, event: E, signal: N) -> Unit
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
