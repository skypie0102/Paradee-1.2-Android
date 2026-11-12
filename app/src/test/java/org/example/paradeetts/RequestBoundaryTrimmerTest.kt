package org.example.paradeetts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestBoundaryTrimmerTest {
    @Test fun trimsOnlyOuterQuietAudioAcrossCallbacks() {
        val leading = FloatArray(300) { 0.0001f }
        val speech1 = FloatArray(500) { 0.3f }
        val middlePause = FloatArray(200)
        val speech2 = FloatArray(700) { -0.3f }
        val trailing = FloatArray(400) { -0.0001f }
        val input = leading + speech1 + middlePause + speech2 + trailing
        val trimmer = RequestBoundaryTrimmer(1000) // 80 samples of padding
        val output = trimmer.accept(input.copyOfRange(0, 600)) +
            trimmer.accept(input.copyOfRange(600, 1300)) +
            trimmer.accept(input.copyOfRange(1300, input.size)) + trimmer.finish()

        val expected = input.copyOfRange(220, input.size - 320)
        assertArrayEquals(expected, output, 0f)
    }

    @Test fun preservesEntirelyQuietAndVeryShortRequests() {
        val quiet = FloatArray(2000) { 0.0001f }
        val trimmer = RequestBoundaryTrimmer(1000)
        assertArrayEquals(quiet, trimmer.accept(quiet) + trimmer.finish(), 0f)

        val speech = floatArrayOf(0f, 0.25f, 0f)
        val short = RequestBoundaryTrimmer(1000)
        assertArrayEquals(speech, short.accept(speech) + short.finish(), 0f)
    }

    @Test fun doesNotLoseAudioWhenAnEarlyCallbackIsSilent() {
        val quietFirst = FloatArray(200)
        val speech = FloatArray(100) { 0.4f }
        val trimmer = RequestBoundaryTrimmer(1000)
        val output = trimmer.accept(quietFirst) + trimmer.accept(speech) + trimmer.finish()
        assertArrayEquals(quietFirst + speech, output, 0f)
    }

    @Test fun emitsLongRequestsBeforeTheLastCallback() {
        val trimmer = RequestBoundaryTrimmer(1000)
        val early = trimmer.accept(FloatArray(2000) { 0.4f })
        assertTrue(early.isNotEmpty())
        assertArrayEquals(FloatArray(2000) { 0.4f }, early + trimmer.finish(), 0f)
    }
}
