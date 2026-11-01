package org.example.paradeetts

internal const val DEFAULT_PAUSE_CUT_PERCENT = 10
internal const val MAX_PAUSE_CUT_PERCENT = 20
internal const val PAUSE_CUT_STEP_PERCENT = 5

/** Keep saved or out-of-range values on one of the slider's five positions. */
internal fun normalizePauseCut(percent: Int): Int {
    val bounded = percent.coerceIn(0, MAX_PAUSE_CUT_PERCENT)
    return ((bounded + PAUSE_CUT_STEP_PERCENT / 2) / PAUSE_CUT_STEP_PERCENT) * PAUSE_CUT_STEP_PERCENT
}

/** The selected value is removed; sherpa's scale is the fraction retained. */
internal fun silenceScaleForCut(percent: Int): Float =
    (100 - normalizePauseCut(percent)) / 100.0f
