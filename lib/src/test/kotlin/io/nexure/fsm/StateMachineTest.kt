package io.nexure.fsm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Semaphore

class StateMachineTest {
    @Test
    fun `test list of all states`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E1)
            .connect(State.S3, State.S4, Event.E1)
            .build()

        assertEquals(State.values().toSet(), fsm.states())
    }

    @Test
    fun `test single initial state`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(State.S1, fsm.initialState())
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on multiple initial states`() {
        StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .initial(State.S2)
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S2, State.S3, Event.E3)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on no initial states`() {
        StateMachine.builder<State, Event, Unit>()
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S2, State.S3, Event.E3)
            .build()
    }

    @Test
    fun `test list of single terminal state`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(setOf(State.S2), fsm.terminalStates())
    }

    @Test
    fun `test list of multiple terminal states`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E3)
            .build()

        assertEquals(setOf(State.S2, State.S3), fsm.terminalStates())
    }

    @Test
    fun `test execution on signal on state change`() {
        var n: Int = 0

        fun plus(signal: Int) {
            n += signal
        }

        val fsm = StateMachine.builder<Char, Event, Int>()
            .initial('a')
            .connect('a', 'b', Event.E2, ::plus)
            .connect('b', 'c', Event.E3, ::plus)
            .build()

        assertEquals(Executed('b'), fsm.onEvent('a', Event.E2, 2))
        assertEquals(Executed('c'), fsm.onEvent('b', Event.E3, 3))

        assertEquals(5, n)
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on duplicate transition`() {
        StateMachine.builder<Char, Event, Int>()
            .initial('a')
            .connect('a', 'b', Event.E1)
            .connect('b', 'c', Event.E2)
            .connect('b', 'c', Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on state machine that is not connected`() {
        StateMachine.builder<Char, Event, Int>()
            .initial('a')
            .connect('a', 'b', Event.E1)
            .connect('c', 'd', Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `throw on same source state and event twice for different target state`() {
        StateMachine.builder<State, Event, Int>()
            .initial(State.S1)
            // Same state (S1) and event (E2) twice, but for different target states
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `throw on same source state and event twice for same target state`() {
        StateMachine.builder<State, Event, Int>()
            .initial(State.S1)
            // Same state (S1) and event (E2) twice with same target state (S2),
            // but with different actions
            .connect(State.S1, State.S2, Event.E2) {
                // do X
            }
            .connect(State.S1, State.S2, Event.E2) {
                // do Y
            }
            .build()
    }

    @Test
    fun `test interceptor changes signal`() {
        val fsm = StateMachine.builder<State, Event, Int>()
            .intercept { _, _, _, signal -> signal * 10 }
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E2) { assertEquals(100, it) }
            .build()

        fsm.onEvent(State.S1, Event.E2, 10)
    }

    @Test
    fun `test post interceptor is executed`() {
        var value: Int = 0
        val fsm = StateMachine.builder<State, Event, Int>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .postIntercept { _, _, _, signal -> value += signal }
            .build()

        fsm.onEvent(State.S1, Event.E2, 32)
        assertEquals(32, value)
    }

    @Test
    fun `test transition with Action`() {
        val semaphore = Semaphore(10)
        val fsm = StateMachine.builder<State, Event, Int>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1) { i ->
                semaphore.tryAcquire(i)
            }
            .build()

        fsm.onEvent(State.S1, Event.E1, 4)
        assertEquals(6, semaphore.availablePermits())
    }

    @Test
    fun `test on event runs action`() {
        var executed = false
        val fsm = StateMachine.builder<State, Event, Boolean>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1) { signal -> executed = signal }
            .build()

        fsm.onEvent(State.S1, Event.E1, true)
        assertTrue(executed)
    }

    @Test
    fun `test going back to initial state is permitted`() {
        val fsm = StateMachine.builder<State, Event, Boolean>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S1, Event.E2)
            .build()

        assertEquals(Executed(State.S2), fsm.onEvent(State.S1, Event.E1, false))
        assertEquals(Executed(State.S1), fsm.onEvent(State.S2, Event.E2, false))
    }

    @Test
    fun `test going back to same state is permitted`() {
        val fsm = StateMachine.builder<State, Event, Boolean>()
            .initial(State.S1)
            .connect(State.S1, State.S1, Event.E1)
            .build()

        assertEquals(Executed(State.S1), fsm.onEvent(State.S1, Event.E1, false))
    }

    @Test
    fun `test reduce state`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
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
        val fsm = StateMachine.builder<State, Event, Unit>()
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
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        val state: State = fsm.reduceState(emptyList())
        assertEquals(State.S1, state)
    }

    @Test
    fun `test reduce state with invalid event for state should be ignored`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E2)
            .build()

        val state: State = fsm.reduceState(listOf(Event.E2))
        assertEquals(State.S1, state)
    }

    @Test
    fun `test rejected transition`() {
        val fsm = StateMachine.builder<State, Event, Boolean>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(Rejected, fsm.onEvent(State.S1, Event.E3, false))
    }

    @Test(expected = StackOverflowError::class)
    fun `test throwable in action is not caught`() {
        val exception = StackOverflowError("foo")
        val fsm = StateMachine.builder<State, Event, Boolean>()
            .initial(State.S1)
            .connect(State.S1, State.S2, Event.E1) { throw exception }
            .build()

        fsm.onEvent(State.S1, Event.E1, false)
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
