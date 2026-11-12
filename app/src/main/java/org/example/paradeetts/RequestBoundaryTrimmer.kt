package org.example.paradeetts

import kotlin.math.abs

/** Conservatively trim only very quiet audio at the outer edges of one request. */
internal class RequestBoundaryTrimmer(sampleRate: Int) {
    private val paddingSamples = (sampleRate * 80L / 1000).toInt().coerceAtLeast(1)
    private val tailCapacity = sampleRate.coerceAtLeast(1)
    private val quietThreshold = 0.0003f
    private var first = true
    private var heardSignal = false
    private var tail = FloatArray(0)

    fun accept(samples: FloatArray): FloatArray {
        if (samples.isEmpty()) return samples
        val firstSignal = samples.indexOfFirst { abs(it) > quietThreshold }
        if (firstSignal >= 0) heardSignal = true
        val start = if (first && firstSignal >= 0) (firstSignal - paddingSamples).coerceAtLeast(0) else 0
        first = false
        val merged = FloatArray(tail.size + samples.size - start)
        tail.copyInto(merged)
        samples.copyInto(merged, tail.size, start)
        val emit = (merged.size - tailCapacity).coerceAtLeast(0)
        tail = merged.copyOfRange(emit, merged.size)
        return merged.copyOfRange(0, emit)
    }

    fun finish(): FloatArray {
        if (!heardSignal) return tail.also { tail = FloatArray(0) }
        val lastSignal = tail.indexOfLast { abs(it) > quietThreshold }
        val keep = if (lastSignal < 0) minOf(paddingSamples, tail.size)
                   else minOf(tail.size, lastSignal + 1 + paddingSamples)
        return tail.copyOfRange(0, keep).also { tail = FloatArray(0) }
    }
}
