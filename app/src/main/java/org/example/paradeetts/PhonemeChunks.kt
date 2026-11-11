package org.example.paradeetts

private val sentenceBoundary = Regex("(?<=[.!?…])\\s+|\\n+")

/** Match the sentence-oriented frontend used by the original Paradee package. */
internal fun splitSpeechSentences(text: String): List<String> =
    text.trim().split(sentenceBoundary).map { it.trim() }.filter { it.isNotEmpty() }

internal fun phonemeCodepoints(text: String): IntArray {
    val result = IntArray(text.codePointCount(0, text.length))
    var offset = 0
    for (i in result.indices) {
        val cp = Character.codePointAt(text, offset)
        result[i] = cp
        offset += Character.charCount(cp)
    }
    return result
}

/** Split at spaces when possible so the model remains below its 512-token limit. */
internal fun chunkPhonemes(phonemes: IntArray, limit: Int = 480): List<IntArray> {
    require(limit > 0)
    val chunks = mutableListOf<IntArray>()
    var begin = 0
    while (begin < phonemes.size) {
        var end = minOf(begin + limit, phonemes.size)
        if (end < phonemes.size) {
            val space = (end - 1 downTo begin + 1).firstOrNull { phonemes[it] == 32 }
            if (space != null) end = space + 1
        }
        chunks.add(phonemes.copyOfRange(begin, end))
        begin = end
    }
    return chunks
}
