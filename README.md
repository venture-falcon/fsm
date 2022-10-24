# fsm
**fsm** is a library to create finite state machines on the JVM.

 > A finite-state machine (FSM) or finite-state automaton (FSA, plural: automata), finite automaton, or simply a state machine, is a mathematical model of computation. It is an abstract machine that can be in exactly one of a finite number of states at any given time. The FSM can change from one state to another in response to some inputs; the change from one state to another is called a transition. An FSM is defined by a list of its states, its initial state, and the inputs that trigger each transition.

From [Finite state machine](https://en.wikipedia.org/wiki/Finite-state_machine) on Wikipedia

## Add Dependency
First **make sure to register this Git repository as a Maven repository**. Check the documentation for [using a published package](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package) on GitHub if you are unsure how to do it. 

Then include the dependency in your build.gradle(.kts) file.
```kotlin
implementation("io.nexure:fsm:2.0.0")
```

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

See [ExampleStateMachineTest.kt](lib/src/test/kotlin/io/nexure/fsm/ExampleStateMachine.kt) for an
example of how a state machine with the above states and transitions is built, and how it can
be invoked to execute certain actions on a given state transition.
