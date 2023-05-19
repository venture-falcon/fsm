package io.nexure.fsm

/**
 * - [S] - the type of state that the state machine handles
 * - [E] - the type of events that the can trigger state changes
 */
sealed class StateMachineBuilder<S : Any, E : Any> {
    class Uninitialized<S : Any, E : Any> internal constructor(
        private val transitions: List<Edge<S, E>> = emptyList(),
    ) : StateMachineBuilder<S, E>() {
        /**
         * Set the initial state for this state machine. There can only one initial state,
         * no more or less.
         */
        fun initial(state: S): Initialized<S, E> = Initialized(state, transitions)
    }

    class Initialized<S : Any, E : Any> internal constructor(
        private val initialState: S,
        private val transitions: List<Edge<S, E>>,
    ) : StateMachineBuilder<S, E>() {
        /**
         * Create a state transition from [source] state to [target] state that will be triggered by
         * [event]. There can be multiple events that connect [source] and [target],
         * but there must never be any ambiguous transitions.
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
        ): Initialized<S, E> = connect(Edge(source, target, event))

        private fun connect(edge: Edge<S, E>): Initialized<S, E> =
            Initialized(initialState, transitions.plus(edge))

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
        fun build(): StateMachine<S, E> {
            StateMachineValidator.validate(initialState, transitions)

            return StateMachineImpl(
                initialState,
                transitions,
            )
        }
    }

    companion object {
        operator fun <S : Any, E : Any> invoke(): Uninitialized<S, E> = Uninitialized()
    }
}
//
///**
// * - [S] - the type of state that the state machine handles
// * - [E] - the type of events that the can trigger state changes
// */
//class StateMachineBuilder<S : Any, E : Any> private constructor(
//    private var initialState: S? = null,
//    private val transitions: List<Edge<S, E>> = emptyList(),
//) {
//    constructor() : this(null, emptyList())
//
//    /**
//     * Set the initial state for this state machine. There must be exactly one initial state,
//     * no more or less. Failing to set an initial state for a state machine will cause an
//     * [InvalidStateMachineException] to be thrown when [build()] is invoked.
//     *
//     * Calling this method more than once, with a different initial state will also cause an
//     * [InvalidStateMachineException] to be thrown, but immediately upon the second call to this
//     * method rather when the state machine is built.
//     */
//    @Throws(InvalidStateMachineException::class)
//    fun initial(state: S): StateMachineBuilder<S, E> {
//        return if (initialState == null) {
//            StateMachineBuilder(state, transitions)
//        } else if (state === initialState) {
//            StateMachineBuilder(initialState, transitions)
//        } else {
//            throw InvalidStateMachineException("There can only be one initial state")
//        }
//    }
//
//    /**
//     * Create a state transition from [source] state to [target] state that will be triggered by
//     * [event]. There can be multiple events that connect [source] and [target],
//     * but there must never be any ambiguous transitions.
//     *
//     * For example, having both of the following transitions, would NOT be permitted
//     * - `(S1, E1) -> S2`
//     * - `(S1, E1) -> S3`
//     *
//     * since it would not be clear if the new state should be `S2` or `S3` when event `E1` is
//     * received.
//     */
//    fun connect(
//        source: S,
//        target: S,
//        event: E,
//    ): StateMachineBuilder<S, E> = connect(Edge(source, target, event))
//
//    private fun connect(edge: Edge<S, E>): StateMachineBuilder<S, E> =
//        StateMachineBuilder(initialState, transitions.plus(edge))
//
//    /**
//     * @throws InvalidStateMachineException if the configured state machine is not valid. The main
//     * reasons for a state machine not being valid are:
//     * - No initial state
//     * - More than one initial state
//     * - The state machine is not connected (some states are not possible to reach from the initial
//     * state)
//     * - The same source state and event is defined twice
//     */
//    @Throws(InvalidStateMachineException::class)
//    fun build(): StateMachine<S, E> {
//        val initState: S = initialState
//            ?: throw InvalidStateMachineException("No initial state set for state machine")
//
//        StateMachineValidator.validate(initState, transitions)
//
//        return StateMachineImpl(
//            initState,
//            transitions,
//        )
//    }
//}
