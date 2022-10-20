package io.nexure.fsm

internal class StateMachineImpl<S : Any, E : Any, N : Any>(
    private val transitions: List<Connection<S, E, N>>,
    private val interceptors: List<(S?, S, E?, N) -> (N)>,
    private val postInterceptors: List<(S?, S, E?, N) -> Unit>
) : StateMachine<S, E, N> {
    private val allowedTransitions: Map<S?, Set<Pair<S, E?>>> = transitions
        .groupBy { it.source() }
        .map { it.key to it.value.map { edge -> edge.target() to edge.event() }.toSet() }
        .toMap()

    private val nonTerminalStates: Set<S> = allowedTransitions.keys.filterNotNull().toSet()

    private val initialState: S = transitions
        .filterIsInstance<Connection.Initial<S, E, N>>()
        .map { it.target }
        .singleOrNull() ?: error("State machine most have exactly one initial state")

    private val initialActions: Map<S, (N) -> Unit> = transitions
        .filterIsInstance<Connection.Initial<S, E, N>>()
        .associate { it.target to it.action }

    private val transitionActions: Map<Triple<S, E, S>, (N) -> Unit> = transitions
        .filterIsInstance<Connection.Transition<S, E, N>>()
        .associate { Triple(it.source, it.event, it.target) to it.action }

    override fun states(): Set<S> = transitions.asSequence()
        .map { listOf(it.source(), it.target()) }
        .flatten()
        .filterNotNull()
        .distinct()
        .toSet()

    override fun initialState(): S = initialState
    override fun terminalStates(): Set<S> = states().minus(nonTerminalStates)

    private fun executeTransition(current: S, next: S, event: E, signal: N) {
        val action: (N) -> Unit = transitionActions[Triple(current, event, next)]
            ?: illegalStateChange(current, next, event)
        val interceptedSignal: N = runInterception(current, next, event, signal)
        action.invoke(interceptedSignal)
        postIntercept(current, next, event, interceptedSignal)
    }

    override fun onInitial(signal: N): S {
        val action: (N) -> Unit = initialActions[initialState] ?: illegalStateChange(initialState)
        val interceptedSignal: N = runInterception(null, initialState, null, signal)
        action.invoke(interceptedSignal)
        postIntercept(null, initialState, null, interceptedSignal)
        return initialState
    }

    private fun runInterception(current: S?, next: S, event: E?, signal: N): N {
        return interceptors.fold(signal) { acc, operation ->
            operation(current, next, event, acc)
        }
    }

    private fun postIntercept(current: S?, next: S, event: E?, signal: N) {
        postInterceptors.forEach { intercept -> intercept(current, next, event, signal) }
    }

    override fun onEvent(current: S, event: E, signal: N): S {
        val next: S = nextState(current, event) ?: illegalEventForState(current, event)
        executeTransition(current, next, event, signal)
        return next
    }

    private fun nextState(current: S, event: E): S? {
        val targets: Set<Pair<S, E?>> = allowedTransitions.getOrDefault(current, emptySet())
        return targets.firstOrNull { it.second == event }?.first
    }
}

private fun <S : Any> illegalStateChange(target: S): Nothing =
    throw IllegalTransitionException.forInitialState(target)

private fun <S : Any> illegalStateChange(from: S?, target: S, event: Any): Nothing =
    throw IllegalTransitionException.forState(from, target, event)

private fun <S : Any> illegalEventForState(from: S?, event: Any?): Nothing =
    throw IllegalTransitionException.forEvent(from, event)
