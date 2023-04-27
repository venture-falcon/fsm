package io.nexure.fsm

internal class StateMachineImpl<S : Any, E : Any, N : Any>(
    private val initialState: S,
    private val transitions: List<Edge<S, E, N>>,
    private val interceptors: List<(S, S, E, N) -> (N)>,
    private val postInterceptors: List<(S, S, E, N) -> Unit>,
) : StateMachine<S, E, N> {
    private val allowedTransitions: Map<S?, Set<Pair<S, E?>>> = transitions
        .groupBy { it.source }
        .map { it.key to it.value.map { edge -> edge.target to edge.event }.toSet() }
        .toMap()

    private val nonTerminalStates: Set<S> = allowedTransitions.keys.filterNotNull().toSet()

    private val transitionActions: Map<Triple<S, E, S>, suspend (N) -> Unit> = transitions
        .associate { Triple(it.source, it.event, it.target) to it.action }

    override fun states(): Set<S> = transitions.asSequence()
        .map { listOf(it.source, it.target) }
        .flatten()
        .distinct()
        .toSet()

    override fun initialState(): S = initialState
    override fun terminalStates(): Set<S> = states().minus(nonTerminalStates)

    override fun reduceState(events: List<E>): S =
        events.fold(initialState) { state, event -> nextState(state, event) ?: state }

    private suspend fun executeTransition(source: S, target: S, event: E, signal: N) {
        val action: suspend (N) -> Unit = transitionActions[Triple(source, event, target)] ?: return
        val interceptedSignal: N = runInterception(source, target, event, signal)
        action(interceptedSignal)
        postIntercept(source, target, event, interceptedSignal)
    }

    private fun runInterception(source: S, target: S, event: E, signal: N): N {
        return interceptors.fold(signal) { acc, operation ->
            operation(source, target, event, acc)
        }
    }

    private fun postIntercept(source: S, target: S, event: E, signal: N) {
        postInterceptors.forEach { intercept -> intercept(source, target, event, signal) }
    }

    override suspend fun onEvent(state: S, event: E, signal: N): Transition<S> {
        val next: S = nextState(state, event) ?: return Rejected
        executeTransition(state, next, event, signal)
        return Executed(next)
    }

    private fun nextState(source: S, event: E): S? {
        val targets: Set<Pair<S, E?>> = allowedTransitions.getOrDefault(source, emptySet())
        return targets.firstOrNull { it.second == event }?.first
    }
}
