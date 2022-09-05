package io.nexure.fsm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.LinkedList
import java.util.concurrent.Semaphore

class StateMachineTest {
    @Test
    fun `test list of all states`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E1)
            .connect(State.S3, State.S4, Event.E1)
            .build()

        assertEquals(State.values().toSet(), fsm.states())
    }

    @Test
    fun `test single initial state`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(State.S1, fsm.initialState())
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on multiple initial states`() {
        StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S2)
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
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .build()

        assertEquals(setOf(State.S2), fsm.terminalStates())
    }

    @Test
    fun `test list of multiple terminal states`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E3)
            .build()

        assertEquals(setOf(State.S2, State.S3), fsm.terminalStates())
    }

    @Test
    fun `test from using multiple transitions in one block`() {
        val receivedSignals: MutableList<String> = LinkedList()

        val action = object : Action<String> {
            override fun action(signal: String) {
                receivedSignals.add(signal)
            }
        }
        val fsm = StateMachine.builder<State, Event, String>()
            .connect(State.S1)
            .from(State.S1) {
                connect(State.S2, Event.E2) { signal -> receivedSignals.add(signal) }
                connect(State.S3, Event.E3, action)
            }
            .connect(State.S3, State.S4, Event.E3)
            .build()

        assertTrue(fsm.allowTransition(State.S1, State.S2))
        assertTrue(fsm.allowTransition(State.S1, State.S3))
        assertTrue(fsm.allowTransition(State.S3, State.S4))
        assertTrue(fsm.acceptEvent(State.S1, Event.E2))
        assertTrue(fsm.acceptEvent(State.S1, Event.E3))
        assertTrue(fsm.acceptEvent(State.S3, Event.E3))

        assertEquals(State.S2, fsm.onEvent(State.S1, Event.E2, "foo"))
        assertEquals(State.S3, fsm.onEvent(State.S1, Event.E3, "bar"))

        assertEquals(listOf("foo", "bar"), receivedSignals)
    }

    @Test
    fun `test execution on signal on state change`() {
        var n: Int = 0

        fun plus(signal: Int) {
            n += signal
        }

        val fsm = StateMachine.builder<Char, Event, Int>()
            .connect('a', ::plus)
            .connect('a', 'b', Event.E2, ::plus)
            .connect('b', 'c', Event.E3, ::plus)
            .build()

        assertEquals('a', fsm.onInitial(1))
        assertEquals('b', fsm.onEvent('a', Event.E2, 2))
        assertEquals('c', fsm.onEvent('b', Event.E3, 3))

        assertEquals(6, n)
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on duplicate initial transition`() {
        StateMachine.builder<Char, Event, Int>()
            .connect('a')
            .connect('a')
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on duplicate transition`() {
        StateMachine.builder<Char, Event, Int>()
            .connect('a')
            .connect('a', 'b', Event.E2)
            .connect('a', 'b', Event.E2)
            .build()
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `test throws on state machine that is not connected`() {
        StateMachine.builder<Char, Event, Int>()
            .connect('a')
            .connect('b', 'c', Event.E1)
            .build()
    }

    @Test
    fun `test accept for allowTransition`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E1)
            .build()

        assertTrue(fsm.allowTransition(State.S1, State.S2))
        assertTrue(fsm.allowTransition(State.S2, State.S3))
    }

    @Test
    fun `test reject for allowTransition`() {
        val fsm = StateMachine.builder<State, Event, Unit>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1)
            .connect(State.S2, State.S3, Event.E1)
            .build()

        assertFalse(fsm.allowTransition(State.S1, State.S3))
        assertFalse(fsm.allowTransition(State.S1, State.S1))

        assertFalse(fsm.allowTransition(State.S2, State.S1))
        assertFalse(fsm.allowTransition(State.S2, State.S2))

        assertFalse(fsm.allowTransition(State.S3, State.S1))
        assertFalse(fsm.allowTransition(State.S3, State.S2))
        assertFalse(fsm.allowTransition(State.S3, State.S3))
    }

    @Test
    fun `test get next state`() {
        val fsm = StateMachine.builder<State, Event, Int>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E3)
            .build()

        assertEquals(State.S2, fsm.nextState(State.S1, Event.E2))
    }

    @Test(expected = InvalidStateMachineException::class)
    fun `throw on same source state and event twice for different target state`() {
        StateMachine.builder<State, Event, Int>()
            .connect(State.S1)
            // Same state (S1) and event (E2) twice, but for different target states
            .connect(State.S1, State.S2, Event.E2)
            .connect(State.S1, State.S3, Event.E2)
            .build()
    }

    @Test
    fun `test interceptor changes signal`() {
        val fsm = StateMachine.builder<State, Event, Int>()
            .intercept { _, _, _, signal -> signal * 10 }
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E2) { assertEquals(100, it) }
            .build()

        fsm.onInitial(10)
    }

    @Test
    fun `test post interceptor is executed`() {
        var value: Int = 0
        val fsm = StateMachine.builder<State, Event, Int>()
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E2)
            .postIntercept { _, _, _, signal -> value += signal }
            .build()

        fsm.onInitial(10)
        fsm.onEvent(State.S1, Event.E2, 32)
        assertEquals(42, value)
    }

    @Test
    fun `test transition with Action`() {
        val semaphore = Semaphore(10)
        val fsm = StateMachine.builder<State, Event, Int>()
            .connect(State.S1)
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
            .connect(State.S1)
            .connect(State.S1, State.S2, Event.E1) { signal -> executed = signal }
            .build()

        fsm.onEvent(State.S1, Event.E1, true)
        assertTrue(executed)
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
