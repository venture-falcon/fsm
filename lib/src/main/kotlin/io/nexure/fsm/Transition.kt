package io.nexure.fsm

/**
 * Outcome of processing of event by state machine. This will indicate if an event was accepted or
 * rejected by the state machine.
 */
sealed class Transition<out S : Any> {
    fun transitioned(): Boolean = this is Accepted<S>

    /**
     * Returns
     * - The new state if the transition was permitted and successful ([Accepted])
     * - `null` if the transitioned was rejected ([Rejected])
     */
    fun stateOrNull(): S? {
        return when (this) {
            is Accepted -> this.state
            Rejected -> null
        }
    }

    /**
     * Invoke this lambda if a transition was executed and successful
     */
    inline fun onTransition(action: (state: S) -> Unit): Transition<S> {
        if (this is Accepted) {
            action(this.state)
        }
        return this
    }

    /**
     * Invoke this lambda if a transition was rejected by the state machine
     */
    inline fun onRejection(handle: () -> Unit): Transition<S> {
        if (this is Rejected) {
            handle()
        }
        return this
    }
}

/**
 * The event was accepted and a state transition occurred. The [state] property reflects the new state.
 */
data class Accepted<S : Any>(val state: S) : Transition<S>() {
    override fun toString(): String = "Executed($state)"
}

/**
 * The event was rejected because the event was not permitted by the state machine in the current
 * state.
 */
object Rejected : Transition<Nothing>() {
    override fun toString(): String = "Rejected"
}
