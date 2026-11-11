package org.example.paradeetts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonemeChunksTest {
    @Test fun sentencesKeepPunctuationAndSkipBlankLines() {
        assertEquals(listOf("Hello!", "How are you?", "Fine."), splitSpeechSentences("  Hello! How are you?\n\nFine. "))
    }

    @Test fun titlesAndAbbreviationsStayWithTheirSentences() {
        assertEquals(
            listOf("Dr. Smith met Mrs. Jones.", "Prof. Lee lives at No. 5."),
            splitSpeechSentences("Dr. Smith met Mrs. Jones. Prof. Lee lives at No. 5.")
        )
        assertEquals(
            listOf("Try fruit, e.g. apples, etc. for lunch.", "They meet at 7 p.m. tomorrow."),
            splitSpeechSentences("Try fruit, e.g. apples, etc. for lunch. They meet at 7 p.m. tomorrow.")
        )
        assertEquals(
            listOf("J. R. R. Tolkien wrote books.", "Dr. J. Smith agrees."),
            splitSpeechSentences("J. R. R. Tolkien wrote books. Dr. J. Smith agrees.")
        )
    }

    @Test fun terminalAbbreviationsAndExplicitLineBreaksStillSplit() {
        assertEquals(
            listOf("We arrived at 7 p.m.", "Then we left.", "We chose plan A.", "That worked."),
            splitSpeechSentences("We arrived at 7 p.m. Then we left. We chose plan A. That worked.")
        )
        assertEquals(listOf("Dr.", "Smith is here."), splitSpeechSentences("Dr.\nSmith is here."))
    }

    @Test fun quotesEllipsesAndDecimalsKeepTheirPunctuation() {
        assertEquals(
            listOf("She said, \"Really?!\"", "Yes…", "It costs 3.14 dollars."),
            splitSpeechSentences("She said, \"Really?!\" Yes… It costs 3.14 dollars.")
        )
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
