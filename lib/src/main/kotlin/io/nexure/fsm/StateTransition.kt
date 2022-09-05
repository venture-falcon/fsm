package io.nexure.fsm

/**
 * StateTransition supports the "old" model where each transition and its associated
 * action can be defined in a file of its own. This also allows us to have backwards
 * compatibility with [io.nexure.orderservice.service.actions.StateMachineAction] and
 * reuse the old StateMachineActions if we prefer that.
 */
interface StateTransition<S : Any, E : Any, N : Any> : Action<N> {
    fun current(): S
    fun next(): S
    fun onEvents(): List<E>
}
