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

fun buildExampleStateMachine(): StateMachine<PaymentState, PaymentEvent> {
    return StateMachineBuilder<PaymentState, PaymentEvent>()
        //       ┏━ Initial state
        .initial(PaymentState.Created)
        //       ┏━ Source state       ┏━ Target state       ┏━ Event triggering transition
        .connect(PaymentState.Created, PaymentState.Pending, PaymentEvent.PaymentSubmitted)
        .connect(PaymentState.Pending, PaymentState.Authorized, PaymentEvent.BankAuthorization)
        .connect(PaymentState.Pending, PaymentState.Refused, PaymentEvent.BankRefusal)
        .connect(PaymentState.Authorized, PaymentState.Settled, PaymentEvent.FundsMoved)
        // This will be called *after* every state transition
        .postIntercept { source, target, event ->
            println("Transitioned from $source to $target due to event $event")
        }
        .build()
}

suspend fun callExampleStateMachine(fsm: StateMachine<PaymentState, PaymentEvent>) {
    val payment = PaymentData("foo", 42)

    //val state0 = fsm.initialize(payment)
    //assertEquals(PaymentState.Created, state0)

    // Transition from state CREATED into state PENDING
    val state1 = fsm.onEvent(PaymentState.Created, PaymentEvent.PaymentSubmitted)
    assertEquals(Executed(PaymentState.Pending), state1)

    // Transition from state PENDING into state AUTHORIZED
    val state2 = fsm.onEvent(PaymentState.Pending, PaymentEvent.BankAuthorization) {
        // Invoke some optional action when payment was authorized
    }
    assertEquals(Executed(PaymentState.Authorized), state2)

    // Transition from state AUTHORIZED into state SETTLED
    val state3 = fsm.onEvent(PaymentState.Authorized, PaymentEvent.FundsMoved)
    assertEquals(Executed(PaymentState.Settled), state3)
}

class ExampleStateMachineTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testExampleStateMachine() = runTest {
        val fsm: StateMachine<PaymentState, PaymentEvent> = buildExampleStateMachine()
        callExampleStateMachine(fsm)
    }
}
