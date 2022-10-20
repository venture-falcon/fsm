package io.nexure.fsm

/**
 * Outcome of processing of event by state machine
 */
sealed class Transition<out S : Any> {
    fun transitioned(): Boolean {
        return when (this) {
            is Executed -> true
            is Failed, Rejected -> false
        }
    }

    /**
     * Returns
     * - The new state if the transition was permitted and successful ([Executed])
     * - `null` if the transitioned was rejected ([Rejected])
     * - Throws an exception if such occurred ([Failed])
     */
    fun stateOrThrow(): S? {
        return when (this) {
            is Executed -> this.state
            Rejected -> null
            is Failed -> throw this.exception
        }
    }
}

/**
 * The event was successfully processed. Any action associated
 */
data class Executed<S : Any>(val state: S) : Transition<S>() {
    override fun toString(): String = "Executed($state)"
}

/**
 * The event was rejected because the event was not permitted by the state machine in the current
 * state, which means that no part of any state transition actions was not executed.
 */
object Rejected : Transition<Nothing>() {
    override fun toString(): String = "Rejected"
}

/**
 * The event could not be processed due to an exception while executing the action associated
 * with this state transition, or while executing any interceptors, if such are present.
 * Some part of the action may have completed while others may not have.
 */
data class Failed(val exception: Exception) : Transition<Nothing>() {
    override fun toString(): String = "Failed($exception)"
}
