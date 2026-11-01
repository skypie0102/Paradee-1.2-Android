package org.example.paradeetts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseControlTest {
    @Test fun percentagesDescribeAmountRemoved() {
        assertEquals(10, DEFAULT_PAUSE_CUT_PERCENT)
        assertEquals(1.0f, silenceScaleForCut(0), 0.0001f)
        assertEquals(0.9f, silenceScaleForCut(10), 0.0001f)
        assertEquals(0.5f, silenceScaleForCut(50), 0.0001f)
        assertEquals(0.2f, silenceScaleForCut(80), 0.0001f)
        assertEquals(0.01f, silenceScaleForCut(99), 0.0001f)
        assertEquals(1.0f, silenceScaleForCut(100), 0.0001f)
    }

    @Test fun fullCutRemovesOnlyQualifyingQuietRuns() {
        // A 10 Hz sample rate makes the 200 ms threshold two samples.
        val input = floatArrayOf(0f, 0f, 0.5f, 0.005f, -0.7f, 0.01f, 0f, 0.5f, 0f, 0f, 0f)
        val expected = floatArrayOf(0.5f, 0.005f, -0.7f, 0.5f)
        assertArrayEquals(expected, removeQuietPauses(input, 10), 0.0f)
    }

    @Test fun fullCutMatchesSherpaBoundaryRules() {
        assertArrayEquals(floatArrayOf(0.5f), removeQuietPauses(floatArrayOf(0f, 0f, 0.5f), 10), 0.0f)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0f), removeQuietPauses(floatArrayOf(0.5f, 0f, 0f), 10), 0.0f)
        assertArrayEquals(floatArrayOf(), removeQuietPauses(floatArrayOf(0f, 0f, 0f), 10), 0.0f)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0.5f), removeQuietPauses(floatArrayOf(0.5f, 0f, 0.5f), 10), 0.0f)
    }
}
