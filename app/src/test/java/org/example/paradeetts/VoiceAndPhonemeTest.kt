package org.example.paradeetts

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceAndPhonemeTest {
    private fun convert(text: String, british: Boolean = false): String {
        val points = text.codePoints().toArray()
        val result = ParadeePhonemes.convert(points, british)
        return String(result, 0, result.size)
    }

    @Test fun selectedVoiceOverridesCachedSystemVoiceByDefault() {
        for (id in 0..3) assertEquals(id, resolveVoiceIndex("af_heart", id, false))
        assertEquals(3, resolveVoiceIndex("bm_fable", 0, true))
        assertEquals(1, resolveVoiceIndex("unknown", 1, true))
        assertEquals(0, resolveVoiceIndex(null, -1, false))
    }

    @Test fun britishVoicesUsePackagedRpVoice() {
        assertEquals("en-us", espeakVoice(0))
        assertEquals("en-gb-x-rp", espeakVoice(1))
        assertEquals("en-us", espeakVoice(2))
        assertEquals("en-gb-x-rp", espeakVoice(3))
    }

    @Test fun mapsEspeakDiphthongsAndConsonantsForAmericanVoice() {
        assertEquals("həlˈO, hˌW ɑɹ ju dˌuɪŋ tədˈA?", convert("həlˈoʊ, hˌaʊ ɑːɹ juː dˌuːɪŋ tədˈeɪ?"))
        assertEquals("ʤˈʌmps wˈɔTəɹ", convert("dʒˈʌmps wˈɔːɾɚ"))
        assertEquals("ʧ I W Y k", convert("t͡ʃ aɪ aʊ ɔɪ ç"))
    }

    @Test fun mapsReportedWordsFromBundledEspeakVoices() {
        assertEquals("nˈɛvəɹ", convert("nˈɛvɚ"))
        assertEquals("pˈɑTəɹ", convert("pˈɑːɾɚ"))
        assertEquals("pˈɔɹd", convert("pˈoːɹd"))
        assertEquals("nˈɛvə", convert("nˈɛvɐ", true))
        assertEquals("pˈɒtə", convert("pˈɒtɐ", true))
        assertEquals("pˈɔːd", convert("pˈɔːd", true))
        assertEquals("ɐ nˈɛvə ɐn pˈɒtə", convert("ɐ nˈɛvɐ ɐn pˈɒtɐ", true))
    }

    @Test fun mapsBritishDiphthongsAndKeepsVowelLength() {
        assertEquals("bˈɑːθ ɡˌQ nˈAvᵊl wˈɔːtə", convert("bˈɑːθ ɡˌəʊ nˈeɪvəl wˈɔːtə", true))
        assertEquals("ɛː ɪə ði ɐ", convert("eə iə ðɪ ɐ", true))
    }
}
