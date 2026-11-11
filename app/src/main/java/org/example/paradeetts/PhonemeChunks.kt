package org.example.paradeetts

import java.util.Locale

private val sentenceBoundary = Regex("""[.!?…]+["'’”»)\]}]*(?=\s|$)|\n+""")
private val abbreviationAtEnd = Regex("""[\p{L}.]+\.$""")
private val alwaysContinuingAbbreviations = setOf(
    "mr.", "mrs.", "ms.", "dr.", "prof.", "rev.", "hon.",
    "pres.", "gov.", "sen.", "rep.", "gen.", "col.", "capt.", "lt.",
    "sgt.", "adm.", "cmdr.", "e.g.", "i.e.", "vs.", "fig."
)
private val possiblyTerminalAbbreviations = setOf("etc.", "a.m.", "p.m.", "u.s.", "u.k.", "u.n.", "inc.", "ltd.", "jr.", "sr.")
private val nameInitials = Regex("""(?:[A-Z]\.\s*)+""")

/** Split speech without separating common titles and abbreviations from their context. */
internal fun splitSpeechSentences(text: String): List<String> {
    val sentences = mutableListOf<String>()
    var start = 0
    fun add(end: Int) {
        val sentence = text.substring(start, end).trim()
        if (sentence.isNotEmpty()) sentences.add(sentence)
    }
    for (boundary in sentenceBoundary.findAll(text)) {
        val end = boundary.range.last + 1
        if (boundary.value.first() == '\n') {
            add(boundary.range.first)
            start = end
            continue
        }
        if (boundary.value == ".") {
            val next = (end until text.length).firstOrNull { !text[it].isWhitespace() }
            val sameLine = next != null && !text.substring(end, next).contains('\n')
            if (sameLine) {
                val match = abbreviationAtEnd.find(text.substring(start, end))
                val abbreviation = match?.value?.lowercase(Locale.ROOT)
                if (abbreviation in alwaysContinuingAbbreviations ||
                    (abbreviation == "no." && text[next!!].isDigit()) ||
                    (abbreviation in possiblyTerminalAbbreviations && text[next!!].isLowerCase())) {
                    continue
                }
                // A leading chain of initials can be part of a name.
                if (match != null && match.value.length == 2 && match.value[0].isUpperCase() &&
                    text[next!!].isUpperCase()) {
                    val lead = text.substring(start, start + match.range.first).trim()
                    if (lead.isEmpty() || nameInitials.matches(lead) ||
                        lead.lowercase(Locale.ROOT) in alwaysContinuingAbbreviations) continue
                }
            }
        }
        add(end)
        start = end
    }
    add(text.length)
    return sentences
}

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
