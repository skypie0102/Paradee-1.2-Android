package org.example.paradeetts

internal val voiceNames = listOf("af_heart", "bf_isabella", "am_puck", "bm_fable")
internal val voiceLabels = listOf(
    "Heart — American female", "Isabella — British female",
    "Puck — American male", "Fable — British male"
)

/** A selected default is reliable even when a client caches its engine voice. */
internal fun resolveVoiceIndex(requested: String?, selected: Int, allowAppVoices: Boolean): Int {
    val fallback = selected.coerceIn(0, voiceNames.lastIndex)
    if (!allowAppVoices) return fallback
    return voiceNames.indexOf(requested).takeIf { it >= 0 } ?: fallback
}

internal fun isBritishVoice(id: Int) = id == 1 || id == 3

// These are eSpeak voice identifiers, not the Android locale names.
internal fun espeakVoice(id: Int) = if (isBritishVoice(id)) "en-gb-x-rp" else "en-us"
