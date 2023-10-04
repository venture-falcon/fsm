package io.nexure.fsm

internal class StateMachineImpl<S : Any, E : Any>(
    private val initialState: S,
    private val transitions: List<Edge<S, E>>,
) : StateMachine<S, E> {
    private val allowedTransitions: Map<S, Set<Pair<S, E>>> = transitions
        .groupBy { it.source }
        .map { it.key to it.value.map { edge -> edge.target to edge.event }.toSet() }
        .toMap()

    private val nonTerminalStates: Set<S> = allowedTransitions.keys

    override fun states(): Set<S> = transitions.asSequence()
        .map { listOf(it.source, it.target) }
        .flatten()
        .distinct()
        .toSet()

    override fun initialState(): S = initialState
    override fun terminalStates(): Set<S> = states().minus(nonTerminalStates)

    override fun reduceState(events: List<E>): S =
        events.fold(initialState) { state, event -> nextState(state, event) ?: state }

    override fun onEvent(state: S, event: E): Transition<S> {
        val next: S = nextState(state, event) ?: return Rejected
        return Accepted(next)
    }

    override fun acceptedEvents(state: S): Set<E> =
        allowedTransitions.getOrDefault(state, emptySet()).map { it.second }.toSet()

    private fun nextState(source: S, event: E): S? {
        val targets: Set<Pair<S, E>> = allowedTransitions.getOrDefault(source, emptySet())
        return targets.firstOrNull { it.second == event }?.first
    }
}
