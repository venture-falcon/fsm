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

    fun initial(state: S): StateMachineBuilder<S, E, N> {
        return if (initialState == null) {
            StateMachineBuilder(state, transitions, interceptors, postInterceptors)
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

    fun intercept(
        interception: (current: S, next: S, event: E, signal: N) -> N
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(initialState, transitions, interceptors.plus(interception), postInterceptors)

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
