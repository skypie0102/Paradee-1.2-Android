package org.example.paradeetts

import kotlin.math.abs

internal const val DEFAULT_PAUSE_CUT_PERCENT = 10

/** sherpa's scale is the percentage retained; 100% cut is handled separately. */
internal fun silenceScaleForCut(percent: Int): Float {
    val cut = percent.coerceIn(0, 100)
    return if (cut == 100) 1.0f else (100 - cut) / 100.0f
}

/**
 * At 100% cut, remove exactly the quiet intervals sherpa would scale.
 * sherpa's ScaleSilence rejects zero, so leave inference at 1.0 and handle
 * this endpoint here. Quiet samples have |amplitude| <= 0.01; a quiet run
 * ending inside the audio qualifies at >= 200 ms, or > 200 ms at the end.
 */
internal fun removeQuietPauses(samples: FloatArray, sampleRate: Int): FloatArray {
    require(sampleRate > 0)
    val threshold = (sampleRate * 0.2).toInt()
    if (threshold < 1 || samples.size < threshold) return samples
    val output = FloatArray(samples.size)
    var quietStart = -1
    var copyFrom = 0
    var written = 0
    for (i in samples.indices) {
        if (abs(samples[i]) <= 0.01f) {
            if (quietStart == -1) quietStart = i
        } else if (quietStart != -1) {
            if (i - quietStart >= threshold) {
                samples.copyInto(output, written, copyFrom, quietStart)
                written += quietStart - copyFrom
                copyFrom = i
            }
            quietStart = -1
        }
    }
    val copyEnd = if (quietStart != -1 && samples.size - quietStart > threshold) quietStart else samples.size
    samples.copyInto(output, written, copyFrom, copyEnd)
    written += copyEnd - copyFrom
    return if (written == samples.size) samples else output.copyOf(written)
}
