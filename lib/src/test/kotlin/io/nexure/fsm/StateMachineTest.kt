package io.nexure.fsm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Semaphore

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineTest {
    @Test
    fun `test list of all states`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E1)
            .connect(State.S3, State.S4, Event.E1)
            .build()

        assertEquals(State.values().toSet(), fsm.states())
    }

    @Test
    fun `test single initial state`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(State.S1, fsm.initialState())
    }

    @Test
    fun `test list of single terminal state`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(setOf(State.S2), fsm.terminalStates())
    }

    @Test
    fun `test list of multiple terminal states`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E3)
            .build()

        assertEquals(setOf(State.S2, State.S3), fsm.terminalStates())
    }

    @Test
    fun `test execution on signal on state change`() {
        var n: Int = 0

        val fsm = StateMachine.builder<Char, Event>()
            .initial('a')
            .connect('a', 'b', Event.E2)
            .connect('b', 'c', Event.E3)
            .build()

        assertEquals(Accepted('b'), fsm.onEvent('a', Event.E2) { n += 2})
        assertEquals(Accepted('c'), fsm.onEvent('b', Event.E3) { n += 3})

        assertEquals(5, n)
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on duplicate transition`() {
        StateMachine.builder<Char, Event>()
            .initial('a')
            .connect('a', 'b', Event.E1)
            .connect('b', 'c', Event.E2)
            .connect('b', 'c', Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on state machine that is not connected`() {
        StateMachine.builder<Char, Event>()
            .initial('a')
            .connect('a', 'b', Event.E1)
            .connect('c', 'd', Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `throw on same source state and event twice for different target state`() {
        StateMachine.builder<State, Event>()
            .initial(State.S1)
            // Same state (S1) and event (E2) twice, but for different target states
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `throw on same source state and event twice for same target state`() {
        StateMachine.builder<State, Event>()
            .initial(State.S1)
            // Same state (S1) and event (E2) twice with same target state (S2),
            // but with different actions
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S2, Event.E2)
            .build()
    }

    @Test
    fun `test post interceptor can be built and executed`() {
        var value: Int = 0

        fun StateMachine<State, Event>.executeWithCustomPostInterception(
            currentState: State,
            event: Event
        ): Transition<State> {
            return this.onEvent(currentState, event).onTransition { newState ->
                value += 32
                println("State changed from $currentState to $newState")
            }
        }

        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .build()

        fsm.executeWithCustomPostInterception(State.S1, Event.E2)
        assertEquals(32, value)
    }

    @Test
    fun `test transition with Action`() {
        val semaphore = Semaphore(10)
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        fsm.onEvent(State.S1, Event.E1){
            semaphore.tryAcquire(4)
        }

        assertEquals(6, semaphore.availablePermits())
    }

    @Test
    fun `test on event runs action`() {
        var executed = false
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        fsm.onEvent(State.S1, Event.E1).onTransition { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `test on event runs action from non suspend function reference`() {
        var executed = false
        fun toggle(state: State) { executed = !executed }
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        fsm.onEvent(State.S1, Event.E1, ::toggle)
        assertTrue(executed)
    }

    @Test
    fun `test on event runs action calling suspend function`() = runTest {
        var executed = false
        suspend fun toggle() {
            delay(1L)
            executed = !executed
        }
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        fsm.onEvent(State.S1, Event.E1) { toggle() }
        assertTrue(executed)
    }

    @Test
    fun `test going back to initial state is permitted`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S1, Event.E2)
            .build()

        assertEquals(Accepted(State.S2), fsm.onEvent(State.S1, Event.E1))
        assertEquals(Accepted(State.S1), fsm.onEvent(State.S2, Event.E2))
    }

    @Test
    fun `test going back to same state is permitted`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S1, Event.E1)
            .build()

        assertEquals(Accepted(State.S1), fsm.onEvent(State.S1, Event.E1))
    }

    @Test
    fun `test reduce state`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E2)
            .connect(State.S3, State.S4, Event.E3)
            .build()

        val state: State = fsm.reduceState(listOf(Event.E1, Event.E2, Event.E3))
        assertEquals(State.S4, state)
    }

    @Test
    fun `test reduce state with repeated event to be ignored`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E2)
            .connect(State.S3, State.S4, Event.E3)
            .build()

        val state: State = fsm.reduceState(listOf(Event.E1, Event.E2, Event.E2, Event.E3))
        assertEquals(State.S4, state)
    }

    @Test
    fun `test reduce state with empty list to be resolved to initial state`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        val state: State = fsm.reduceState(emptyList())
        assertEquals(State.S1, state)
    }

    @Test
    fun `test reduce state with invalid event for state should be ignored`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E2)
            .build()

        val state: State = fsm.reduceState(listOf(Event.E2))
        assertEquals(State.S1, state)
    }

    @Test
    fun `test rejected transition`() {
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(Rejected, fsm.onEvent(State.S1, Event.E3))
    }

    @Test(expected = StackOverflowError::class)
    fun `test throwable in action is not caught`() {
        val exception = StackOverflowError("foo")
        val fsm = StateMachine.builder<State, Event>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        fsm.onEvent(State.S1, Event.E1) { throw exception }
    }
}

private enum class State {
    S1,
    S2,
    S3,
    S4
}

private enum class Event {
    E1,
    E2,
    E3
}
