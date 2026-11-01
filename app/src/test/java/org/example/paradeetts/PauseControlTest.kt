package org.example.paradeetts

import org.junit.Assert.assertEquals
import org.junit.Test

class PauseControlTest {
    @Test fun percentagesDescribeAmountRemoved() {
        assertEquals(10, DEFAULT_PAUSE_CUT_PERCENT)
        assertEquals(50, MAX_PAUSE_CUT_PERCENT)
        assertEquals(1.0f, silenceScaleForCut(0), 0.0001f)
        assertEquals(0.9f, silenceScaleForCut(10), 0.0001f)
        assertEquals(0.8f, silenceScaleForCut(20), 0.0001f)
        assertEquals(0.5f, silenceScaleForCut(50), 0.0001f)
    }

    @Test fun valuesOutsideRangeAreClamped() {
        assertEquals(1.0f, silenceScaleForCut(-1), 0.0001f)
        assertEquals(0.5f, silenceScaleForCut(51), 0.0001f)
        assertEquals(0.5f, silenceScaleForCut(100), 0.0001f)
    }
}
