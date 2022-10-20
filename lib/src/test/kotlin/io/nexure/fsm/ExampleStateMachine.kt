package io.nexure.fsm

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

data class PaymentData(
    val id: String,
    val amount: Int
)

fun buildExampleStateMachine(): StateMachine<PaymentState, PaymentEvent, PaymentData> {
    return StateMachineBuilder<PaymentState, PaymentEvent, PaymentData>()
        //       ┏━ Initial state
        .initial(PaymentState.Created)
        //       ┏━ Source state       ┏━ Target state       ┏━ Event triggering transition
        .connect(PaymentState.Created, PaymentState.Pending, PaymentEvent.PaymentSubmitted)
        .connect(PaymentState.Pending, PaymentState.Authorized, PaymentEvent.BankAuthorization) {
            // Invoke some optional action when payment was authorized
        }
        .connect(PaymentState.Pending, PaymentState.Refused, PaymentEvent.BankRefusal) {
            // Invoke some optional action when payment was refused
        }
        .connect(PaymentState.Authorized, PaymentState.Settled, PaymentEvent.FundsMoved)
        // This will be called after every state transition
        .postIntercept { previousState, newState, event, _ ->
            println("Transitioned from $previousState to $newState due to event $event")
        }
        .build()
}

fun callExampleStateMachine() {
    val fsm: StateMachine<PaymentState, PaymentEvent, PaymentData> = buildExampleStateMachine()

    val payment = PaymentData("foo", 42)

    // Transition from state CREATED into state PENDING
    fsm.onEvent(PaymentState.Created, PaymentEvent.PaymentSubmitted, payment)
    // Transition from state PENDING into state AUTHORIZED
    fsm.onEvent(PaymentState.Pending, PaymentEvent.BankAuthorization, payment)
    // Transition from state AUTHORIZED into state SETTLED
    fsm.onEvent(PaymentState.Authorized, PaymentEvent.FundsMoved, payment)
}

class ExampleStateMachineTest {
    @Test
    fun testExampleStateMachine() {
        buildExampleStateMachine()
        callExampleStateMachine()
    }
}
