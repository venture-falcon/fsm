package io.nexure.fsm

internal class StateMachineImpl<S : Any, E : Any>(
    private val initialState: S,
    private val transitions: List<Edge<S, E>>,
    private val postInterceptors: List<(S, S, E) -> Unit>
) : StateMachine<S, E> {
    private val allowedTransitions: Map<S?, Set<Pair<S, E?>>> = transitions
        .groupBy { it.source }
        .map { it.key to it.value.map { edge -> edge.target to edge.event }.toSet() }
        .toMap()

    private val nonTerminalStates: Set<S> = allowedTransitions.keys.filterNotNull().toSet()

    override fun states(): Set<S> = transitions.asSequence()
        .map { listOf(it.source, it.target) }
        .flatten()
        .distinct()
        .toSet()

    override fun initialState(): S = initialState
    override fun terminalStates(): Set<S> = states().minus(nonTerminalStates)

    override fun reduceState(events: List<E>): S =
        events.fold(initialState) { state, event -> nextState(state, event) ?: state }

    private fun postIntercept(source: S, target: S, event: E) {
        postInterceptors.forEach { intercept -> intercept(source, target, event) }
    }

//    override suspend fun initialize(signal: N): S {
//        initialAction(signal)
//        return initialState
//    }

    override suspend fun onEvent(state: S, event: E, action: suspend () -> Unit): Transition<S> {
        val next: S = nextState(state, event) ?: return Rejected
        action()
        postIntercept(state, next, event)
        return Executed(next)
    }

    private fun nextState(source: S, event: E): S? {
        val targets: Set<Pair<S, E?>> = allowedTransitions.getOrDefault(source, emptySet())
        return targets.firstOrNull { it.second == event }?.first
    }
}
