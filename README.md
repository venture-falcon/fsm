# fsm
**fsm** is a library to create finite state machines on the JVM.

 > A finite-state machine (FSM) or finite-state automaton (FSA, plural: automata), finite automaton, or simply a state machine, is a mathematical model of computation. It is an abstract machine that can be in exactly one of a finite number of states at any given time. The FSM can change from one state to another in response to some inputs; the change from one state to another is called a transition. An FSM is defined by a list of its states, its initial state, and the inputs that trigger each transition.

From [Finite state machine](https://en.wikipedia.org/wiki/Finite-state_machine) on Wikipedia

 ## Usage
 Here is a fictional example of what a state machine could look like that models the process of a
 payment. In summary it has
 - _Initial_ state `CREATED`
 - _Intermediary_ states `PENDING` and `AUTHORIZED`
 - _Terminal_ states `SETTLED` and `REFUSED`

 ```mermaid
 stateDiagram-v2
     [*] --> CREATED
     CREATED --> PENDING: Submitted by consumer
     PENDING --> AUTHORIZED: Authorized by bank
     PENDING --> REFUSED: Refused by bank
     AUTHORIZED --> SETTLED: Funds moved
     SETTLED --> [*]
     REFUSED --> [*]
 ```

Building said state machine with this library would look something like this

```kotlin
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
        .connect(PaymentState.Created)
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
```

 The state machine could then be called the following way

```kotlin
val fsm: StateMachine<PaymentState, PaymentEvent, PaymentData> = buildExampleStateMachine()

val payment = PaymentData("foo", 42)

// Transition into the initial state CREATED
fsm.onInitial(payment)
// Transition from state CREATED into state PENDING
fsm.onEvent(PaymentState.Created, PaymentEvent.PaymentSubmitted, payment)
// Transition from state PENDING into state AUTHORIZED
fsm.onEvent(PaymentState.Pending, PaymentEvent.BankAuthorization, payment)
// Transition from state AUTHORIZED into state SETTLED
fsm.onEvent(PaymentState.Authorized, PaymentEvent.FundsMoved, payment)
```

This example can also be found in the file [ExampleStateMachineTest.kt](lib/src/test/kotlin/io/nexure/fsm/ExampleStateMachine.kt).