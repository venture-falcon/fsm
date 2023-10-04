package io.nexure.fsm

import org.junit.Assert.assertEquals
import org.junit.Test

enum class PaymentState {
    Created,
    Pending,
    Authorized,
    Settled,
    Refused
}

enum class PaymentEvent {
    PaymentSubmitted,
    BankAuthorization,
    BankRefusal,
    FundsMoved
}

fun buildExampleStateMachine(): StateMachine<PaymentState, PaymentEvent> {
    return StateMachineBuilder<PaymentState, PaymentEvent>()
        //       ┏━ Initial state
        .initial(PaymentState.Created)
        //       ┏━ Source state       ┏━ Target state       ┏━ Event triggering transition
        .connect(PaymentState.Created, PaymentState.Pending, PaymentEvent.PaymentSubmitted)
        .connect(PaymentState.Pending, PaymentState.Authorized, PaymentEvent.BankAuthorization)
        .connect(PaymentState.Pending, PaymentState.Refused, PaymentEvent.BankRefusal)
        .connect(PaymentState.Authorized, PaymentState.Settled, PaymentEvent.FundsMoved)
        .build()
}

fun callExampleStateMachine(fsm: StateMachine<PaymentState, PaymentEvent>) {
    // Transition from state CREATED into state PENDING
    val state1 = fsm.onEvent(PaymentState.Created, PaymentEvent.PaymentSubmitted)
    assertEquals(Accepted(PaymentState.Pending), state1)

    // Transition from state PENDING into state AUTHORIZED
    val state2 = fsm.onEvent(PaymentState.Pending, PaymentEvent.BankAuthorization).onTransition {
        // Invoke some optional action when payment was authorized
    }
    assertEquals(Accepted(PaymentState.Authorized), state2)

    // Transition from state AUTHORIZED into state SETTLED
    val state3 = fsm.onEvent(PaymentState.Authorized, PaymentEvent.FundsMoved)
    assertEquals(Accepted(PaymentState.Settled), state3)
}

class ExampleStateMachineTest {
    @Test
    fun testExampleStateMachine() {
        val fsm: StateMachine<PaymentState, PaymentEvent> = buildExampleStateMachine()
        callExampleStateMachine(fsm)
    }
}
