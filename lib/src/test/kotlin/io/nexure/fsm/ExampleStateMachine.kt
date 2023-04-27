package io.nexure.fsm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
        // This will be called *before* every state transition, with the possibility of altering
        // the input `signal` if needed
        .intercept { source, target, event, signal ->
            println("Will execute transition from $source to $target due to event $event")
            signal
        }
        // This will be called *after* every state transition
        .postIntercept { source, target, event, _ ->
            println("Transitioned from $source to $target due to event $event")
        }
        .build()
}

suspend fun callExampleStateMachine(fsm: StateMachine<PaymentState, PaymentEvent, PaymentData>) {
    val payment = PaymentData("foo", 42)

    // Transition from state CREATED into state PENDING
    val state1 = fsm.onEvent(PaymentState.Created, PaymentEvent.PaymentSubmitted, payment)
    assertEquals(Executed(PaymentState.Pending), state1)

    // Transition from state PENDING into state AUTHORIZED
    val state2 = fsm.onEvent(PaymentState.Pending, PaymentEvent.BankAuthorization, payment)
    assertEquals(Executed(PaymentState.Authorized), state2)

    // Transition from state AUTHORIZED into state SETTLED
    val state3 = fsm.onEvent(PaymentState.Authorized, PaymentEvent.FundsMoved, payment)
    assertEquals(Executed(PaymentState.Settled), state3)
}

class ExampleStateMachineTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testExampleStateMachine() = runTest {
        val fsm: StateMachine<PaymentState, PaymentEvent, PaymentData> = buildExampleStateMachine()
        callExampleStateMachine(fsm)
    }
}
