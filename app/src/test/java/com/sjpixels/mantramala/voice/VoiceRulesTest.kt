package com.sjpixels.mantramala.voice

import com.sjpixels.mantramala.data.VoiceCounterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRulesTest {
    @Test fun eachPlayCountsOne() {
        val s = VoiceRules.afterPlay(VoiceCounterState("om", 10, 500), now = 9_000, pauseEachMala = true)
        assertEquals(VoiceCounterState("om", 11, 500), s.state)
        assertFalse(s.malaCompleted)
        assertTrue(s.keepPlaying)
    }

    @Test fun play108CompletesMalaAndPausesWhenAsked() {
        val s = VoiceRules.afterPlay(VoiceCounterState("om", 107, 500), now = 9_000, pauseEachMala = true)
        assertTrue(s.malaCompleted)
        assertEquals(VoiceCounterState("om", 0, 0), s.state)
        assertEquals(500L, s.roundStartedAt)
        assertFalse(s.keepPlaying)
    }

    @Test fun autoPlayKeepsGoingAfterMala() {
        var state = VoiceCounterState("om")
        var malas = 0
        repeat(108 * 2 + 3) { i ->
            val step = VoiceRules.afterPlay(state, now = i + 1L, pauseEachMala = false)
            assertTrue(step.keepPlaying)
            if (step.malaCompleted) malas++
            state = step.state
        }
        assertEquals(2, malas)
        assertEquals(3, state.count)
    }

    @Test fun anotherMantraStartsFromZero() {
        val saved = VoiceCounterState("om", 40, 100)
        assertEquals(saved, VoiceRules.forMantra(saved, "om"))
        assertEquals(VoiceCounterState("gayatri"), VoiceRules.forMantra(saved, "gayatri"))
    }
}
