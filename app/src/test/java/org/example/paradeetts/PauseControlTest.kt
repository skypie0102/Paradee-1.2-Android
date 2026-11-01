package org.example.paradeetts

import org.junit.Assert.assertEquals
import org.junit.Test

class PauseControlTest {
    @Test fun percentagesDescribeAmountRemovedInFivePercentSteps() {
        assertEquals(10, DEFAULT_PAUSE_CUT_PERCENT)
        assertEquals(20, MAX_PAUSE_CUT_PERCENT)
        assertEquals(5, PAUSE_CUT_STEP_PERCENT)
        assertEquals(1.0f, silenceScaleForCut(0), 0.0001f)
        assertEquals(0.95f, silenceScaleForCut(5), 0.0001f)
        assertEquals(0.9f, silenceScaleForCut(10), 0.0001f)
        assertEquals(0.85f, silenceScaleForCut(15), 0.0001f)
        assertEquals(0.8f, silenceScaleForCut(20), 0.0001f)
    }

    @Test fun oldAndOutOfRangeValuesAreClampedAndRounded() {
        assertEquals(0, normalizePauseCut(-1))
        assertEquals(0, normalizePauseCut(2))
        assertEquals(5, normalizePauseCut(3))
        assertEquals(10, normalizePauseCut(12))
        assertEquals(15, normalizePauseCut(13))
        assertEquals(20, normalizePauseCut(21))
        assertEquals(20, normalizePauseCut(100))
    }
}
