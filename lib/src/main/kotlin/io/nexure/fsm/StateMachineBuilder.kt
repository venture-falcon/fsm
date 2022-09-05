package io.nexure.fsm

import java.util.LinkedList

@Suppress("UNUSED_PARAMETER")
private fun <N : Any> noOp(signal: N) {}

class StateMachineBuilder<S : Any, E : Any, N : Any> private constructor(
    private val transitions: List<Connection<S, E, N>> = emptyList(),
    private val interceptors: List<(S?, S, E?, N) -> (N)> = emptyList(),
    private val postInterceptors: List<(S?, S, E?, N) -> Unit> = emptyList()
) {
    constructor() : this(emptyList(), emptyList(), emptyList())

    fun connect(
        state: S,
        action: (signal: N) -> Unit = ::noOp
    ): StateMachineBuilder<S, E, N> = connect(Connection.Initial(state, action))

    fun connect(
        current: S,
        next: S,
        event: E,
        action: (signal: N) -> Unit = ::noOp
    ): StateMachineBuilder<S, E, N> = connect(Connection.Transition(current, next, event, action))

    fun connect(current: S, next: S, event: E, action: Action<N>): StateMachineBuilder<S, E, N> =
        connect(current, next, event, action::action)

    private fun connect(connection: Connection<S, E, N>): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(transitions.plus(connection), interceptors, postInterceptors)

    fun from(current: S, apply: ConnectionContext<S, E, N>.() -> Unit): StateMachineBuilder<S, E, N> {
        val context = ConnectionContext<S, E, N>(current)
        context.apply()
        return context.fold(this) { acc, connection -> acc.connect(connection) }
    }

    class ConnectionContext<S : Any, E : Any, N : Any> internal constructor(
        private val current: S,
        private val connections: MutableList<Connection<S, E, N>> = LinkedList()
    ) : Iterable<Connection<S, E, N>> by connections {
        fun connect(next: S, event: E, action: Action<N>) {
            connections.add(Connection.Transition(current, next, event, action::action))
        }

        fun connect(next: S, event: E, action: (signal: N) -> Unit = ::noOp) {
            connections.add(Connection.Transition(current, next, event, action))
        }
    }

    fun intercept(
        interception: (current: S?, next: S, event: E?, signal: N) -> N
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(transitions, interceptors.plus(interception), postInterceptors)

    fun postIntercept(
        interception: (current: S?, next: S, event: E?, signal: N) -> Unit
    ): StateMachineBuilder<S, E, N> =
        StateMachineBuilder(transitions, interceptors, postInterceptors.plus(interception))

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
        StateMachineValidator.validate(transitions)

        return StateMachineImpl(
            transitions,
            interceptors,
            postInterceptors
        )
    }
}
