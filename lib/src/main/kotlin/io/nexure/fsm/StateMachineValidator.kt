package io.nexure.fsm

import java.util.Deque
import java.util.LinkedList

internal object StateMachineValidator {
    fun <S : Any, E : Any> validate(initialState: S, transitions: List<Edge<S, E>>) {
        rejectDuplicates(transitions)
        findIllegalCombinations(transitions)
        isConnected(initialState, transitions)
    }

    /**
     * Check so a combination of source state, target state and event is not defined more than once
     */
    private fun <S : Any, E : Any> rejectDuplicates(transitions: List<Edge<S, E>>) {
        val duplicate: Edge<S, E>? = transitions
            .duplicatesBy { Triple(it.source, it.target, it.event) }
            .firstOrNull()

        validate(duplicate == null) {
            val (initialState, targetState, byEvent) = duplicate!!
            "Transition from $initialState to $targetState via $byEvent occurs twice"
        }
    }

    /**
     * Verify that no invalid combinations of transitions are present. Example of such invalid
     * combination would be where two edges (connections) with the same source state and same event
     * has a different target state, like;
     *
     * Transition 0: S0 --> E0 --> S1
     * Transition 1: S0 --> E0 --> S2
     *
     * These two transitions would not be allowed to exist in the same state machine at the same
     * time.
     */
    private fun <S : Any, E : Any> findIllegalCombinations(transitions: List<Edge<S, E>>) {
        val illegal: Edge<S, E>? = transitions
            .groupBy { it.source }
            .filter { it.value.size > 1 }
            .map { x -> x.value.map { y -> x.value.map { it to y } } }
            .flatten()
            .flatten()
            .firstOrNull { illegalCombination(it.first, it.second) }
            ?.first

        validate(illegal == null) {
            val initialState = illegal!!.source
            val byEvent = illegal.event
            "Transition from $initialState via $byEvent occurs twice"
        }
    }

    /**
     * Return true if this edge and other edge has the same source
     * and event but different target, since a source state which is triggered
     * by a specific event should always result in the same target state.
     */
    private fun <S : Any, E : Any> illegalCombination(e0: Edge<S, E>, e1: Edge<S, E>): Boolean {
        if (e0 === e1) {
            return false
        }
        val sameSource: Boolean = e0.source == e1.source
        val sameTarget: Boolean = e0.target == e1.target
        val sameEvent: Boolean = e0.event == e1.event
        return sameSource && sameEvent && !sameTarget
    }

    /**
     * Validate the configuration of the state machine, making sure that state machine is connected
     */
    private fun <S : Any, E : Any> isConnected(initialState: S, transitions: List<Edge<S, E>>) {
        val stateTransitions: Map<S?, Set<S>> = transitions
            .groupBy { it.source }
            .mapValues { it.value.map { value -> value.target }.toSet() }
            .toMap()

        val allNodes: Collection<S> = stateTransitions.keys.filterNotNull() + stateTransitions.values.flatten()
        val uniqueNodes: Int = allNodes.distinct().size
        validateIsConnected(initialState, stateTransitions, uniqueNodes)
    }

    /**
     * Make sure that the graph representing the state machine is connected. In other words, all states
     * of the state machine must be able to reach through a series of valid transitions according to
     * this state machine.
     */
    private tailrec fun <S : Any> validateIsConnected(
        initialState: S,
        transitions: Map<S?, Set<S>>,
        target: Int,
        visited: Set<S> = setOf(initialState),
        toVisit: Deque<S> = LinkedList(transitions.getOrDefault(initialState, emptyList()))
    ) {
        validate(visited.size == target || toVisit.isNotEmpty()) {
            val remainingNodes: List<S> = transitions.keys.filterNotNull()
            "Unable to reach nodes in state machine: $remainingNodes"
        }

        if (visited.size == target) {
            return
        } else {
            val current: S = toVisit.pop()
            val next: Collection<S> = transitions.getOrDefault(current, emptyList())
            toVisit.addAll(next)
            validateIsConnected(
                initialState,
                transitions = transitions - current,
                target = target,
                visited = visited + current,
                toVisit = toVisit
            )
        }
    }
}

class InvalidStateMachineException(override val message: String) : Exception()

private inline fun validate(predicate: Boolean, message: () -> String) {
    if (!predicate) {
        throw InvalidStateMachineException(message())
    }
}

private fun <T, R> Iterable<T>.duplicatesBy(keySelector: (T) -> R): Iterable<T> =
    this.groupBy(keySelector)
        .filter { it.value.size > 1 }
        .map { it.value.first() }
