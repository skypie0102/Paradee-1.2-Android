package org.example.paradeetts

internal const val DEFAULT_PAUSE_CUT_PERCENT = 10
internal const val MAX_PAUSE_CUT_PERCENT = 50

/** The selected value is removed; sherpa's scale is the fraction retained. */
internal fun silenceScaleForCut(percent: Int): Float =
    (100 - percent.coerceIn(0, MAX_PAUSE_CUT_PERCENT)) / 100.0f
