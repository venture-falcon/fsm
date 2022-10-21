package io.nexure.fsm

internal class StateMachineImpl<S : Any, E : Any, N : Any>(
    private val initialState: S,
    private val transitions: List<Edge<S, E, N>>,
    private val interceptors: List<(S, S, E, N) -> (N)>,
    private val postInterceptors: List<(S, S, E, N) -> Unit>
) : StateMachine<S, E, N> {
    private val allowedTransitions: Map<S?, Set<Pair<S, E?>>> = transitions
        .groupBy { it.source }
        .map { it.key to it.value.map { edge -> edge.target to edge.event }.toSet() }
        .toMap()

    private val nonTerminalStates: Set<S> = allowedTransitions.keys.filterNotNull().toSet()

    private val transitionActions: Map<Triple<S, E, S>, (N) -> Unit> = transitions
        .associate { Triple(it.source, it.event, it.target) to it.action }

    override fun states(): Set<S> = transitions.asSequence()
        .map { listOf(it.source, it.target) }
        .flatten()
        .distinct()
        .toSet()

    override fun initialState(): S = initialState
    override fun terminalStates(): Set<S> = states().minus(nonTerminalStates)

    private fun executeTransition(current: S, next: S, event: E, signal: N) {
        val action: (N) -> Unit = transitionActions[Triple(current, event, next)] ?: return
        val interceptedSignal: N = runInterception(current, next, event, signal)
        action.invoke(interceptedSignal)
        postIntercept(current, next, event, interceptedSignal)
    }

    private fun runInterception(current: S, next: S, event: E, signal: N): N {
        return interceptors.fold(signal) { acc, operation ->
            operation(current, next, event, acc)
        }
    }

    private fun postIntercept(current: S, next: S, event: E, signal: N) {
        postInterceptors.forEach { intercept -> intercept(current, next, event, signal) }
    }

    override fun onEvent(current: S, event: E, signal: N): Transition<S> {
        val next: S = nextState(current, event) ?: return Rejected
        executeTransition(current, next, event, signal)
        return Executed(next)
    }

    private fun nextState(current: S, event: E): S? {
        val targets: Set<Pair<S, E?>> = allowedTransitions.getOrDefault(current, emptySet())
        return targets.firstOrNull { it.second == event }?.first
    }
}
