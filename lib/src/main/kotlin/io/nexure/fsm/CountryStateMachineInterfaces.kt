package io.nexure.fsm

import com.neovisionaries.i18n.CountryCode

interface CountryStateMachineConfig<S : Any, E : Any> {
    val country: CountryCode
    val enabled: Boolean
    val transitions: List<CountryEdge<S, E>>
    val initialState: S
}

interface CountryEdge<S : Any, E : Any> {
    val source: S
    val target: S
    val event: E
}

interface CountryStateMachineFactory<S : Any, E : Any, C : CountryStateMachineConfig<S, E>> {
    suspend fun createFsmForAllCountries(): MutableMap<CountryCode, StateMachine<S, E>>
    fun createStateMachineForCountry(transitions: List<CountryEdge<S, E>>): StateMachine<S, E>?
}
