package org.example.paradeetts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonemeChunksTest {
    @Test fun sentencesKeepPunctuationAndSkipBlankLines() {
        assertEquals(listOf("Hello!", "How are you?", "Fine."), splitSpeechSentences("  Hello! How are you?\n\nFine. "))
    }

    @Test fun longPhonemesAreNeverLost() {
        val input = phonemeCodepoints("ˈɛ".repeat(250) + " " + "p".repeat(500))
        val chunks = chunkPhonemes(input)
        assertTrue(chunks.all { it.size <= 480 })
        assertEquals(input.toList(), chunks.flatMap { it.toList() })
    }

    @Test fun handlesSupplementaryUnicodeWithoutSplittingSurrogates() {
        assertEquals(listOf(0x1f99c, 'a'.code), phonemeCodepoints("🦜a").toList())
    }
}
