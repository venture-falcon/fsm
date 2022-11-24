package io.nexure.fsm

/**
 * Outcome of processing of event by state machine. This will indicate if an event was accepted or
 * rejected by the state machine, or if there was an exception while processing the event.
 */
sealed class Transition<out S : Any> {
    fun transitioned(): Boolean = this is Executed<S>

    /**
     * Returns
     * - The new state if the transition was permitted and successful ([Executed])
     * - `null` if the transitioned was rejected ([Rejected])
     */
    fun stateOrNull(): S? {
        return when (this) {
            is Executed -> this.state
            Rejected -> null
        }
    }

    /**
     * Invoke this lambda if a transition was executed and successful
     */
    inline fun onExecution(handle: (state: S) -> Unit): Transition<S> {
        if (this is Executed) {
            handle(this.state)
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
 * The event was successfully processed. Any action associated with the transition, including
 * interceptors, will have been executed successfully.
 */
data class Executed<S : Any>(val state: S) : Transition<S>() {
    override fun toString(): String = "Executed($state)"
}

/**
 * The event was rejected because the event was not permitted by the state machine in the current
 * state, which means that no part of any state transition action or interceptors were executed.
 */
object Rejected : Transition<Nothing>() {
    override fun toString(): String = "Rejected"
}
