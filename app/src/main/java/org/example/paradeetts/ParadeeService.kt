package org.example.paradeetts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.github.csukuangfj.piper.phonemize.piperPhonemizeInitialize
import com.github.csukuangfj.piper.phonemize.piperPhonemizeText
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class ParadeeService : TextToSpeechService() {
    private var engine: OfflineTts? = null
    private val stopGeneration = AtomicInteger(0)
    @Volatile private var misakiUsed = false

    private fun ensureEngine(): OfflineTts {
        engine?.let { return it }
        // Older versions extracted every eSpeak dictionary into app-private storage.
        // English voices use en_dict; the others are no longer included in the APK.
        filesDir.resolve("espeak-ng-data").listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith("_dict") && file.name != "en_dict" && !file.delete()) {
                Log.w("ParadeeTTS", "Could not remove unused eSpeak dictionary ${file.name}")
            }
        }
        piperPhonemizeInitialize(applicationContext)
        val model = OfflineTtsKokoroModelConfig(model = "paradee/model.onnx", voices = "paradee/voices.bin", tokens = "paradee/tokens.txt")
        return OfflineTts(assets, OfflineTtsConfig(model = OfflineTtsModelConfig(kokoro = model, numThreads = 1), silenceScale = 0.2f)).also { engine = it }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang != "eng" && lang != "en") return TextToSpeech.LANG_NOT_SUPPORTED
        return when (country) {
            "USA", "US", "GBR", "GB" -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            null, "" -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_AVAILABLE
        }
    }
    override fun onGetLanguage(): Array<String> {
        val selected = getSharedPreferences("paradee", MODE_PRIVATE).getInt("voice", 0).coerceIn(0, 3)
        return arrayOf("eng", if (selected == 1 || selected == 3) "GBR" else "USA", "")
    }
    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int = onIsLanguageAvailable(lang, country, variant)
    override fun onGetVoices(): MutableList<Voice> = voiceNames.mapIndexed { i, name ->
        Voice(name, if (i == 1 || i == 3) Locale.UK else Locale.US, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, emptySet())
    }.toMutableList()
    override fun onIsValidVoiceName(voiceName: String?): Int = if (voiceNames.contains(voiceName)) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    override fun onLoadVoice(voiceName: String?): Int = onIsValidVoiceName(voiceName)
    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        if (onIsLanguageAvailable(lang, country, variant) == TextToSpeech.LANG_NOT_SUPPORTED) return null
        val chosen = getSharedPreferences("paradee", MODE_PRIVATE).getInt("voice", 0).coerceIn(0, 3)
        val isUk = country == "GBR" || country == "GB"
        val isUs = country == "USA" || country == "US"
        val index = if (isUk && chosen !in listOf(1, 3)) 1 else if (isUs && chosen in listOf(1, 3)) 0 else chosen
        return voiceNames[index]
    }
    override fun onStop() { stopGeneration.incrementAndGet() }
    override fun onDestroy() {
        if (misakiUsed) runCatching { MisakiBridge.release() }
        engine?.release()
        engine = null
        super.onDestroy()
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val ticket = stopGeneration.get()
        try {
            if (onIsLanguageAvailable(request.language, request.country, request.variant) == TextToSpeech.LANG_NOT_SUPPORTED) { callback.error(); return }
            val tts = ensureEngine()
            if (ticket != stopGeneration.get()) return
            val prefs = getSharedPreferences("paradee", MODE_PRIVATE)
            val id = resolveVoiceIndex(request.voiceName, prefs.getInt("voice", 0), prefs.getBoolean("allow_app_voices", false))
            val text = request.charSequenceText.toString()
            if (callback.start(tts.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return
            if (text.isBlank()) { callback.done(); return }
            val chunks = mutableListOf<IntArray>()
            if (prefs.getString("phonemizer", "misaki") != "espeak") {
                for (sentence in splitSpeechSentences(text)) {
                    if (ticket != stopGeneration.get()) return
                    misakiUsed = true
                    val phonemes = MisakiBridge.phonemize(sentence, isBritishVoice(id))
                    chunks.addAll(chunkPhonemes(phonemeCodepoints(phonemes)))
                }
            } else {
                val result = piperPhonemizeText(text, espeakVoice(id))
                    ?: throw IllegalStateException("Could not phonemize text")
                try {
                    for (i in 0 until result.numSentences) {
                        val phonemes = ParadeePhonemes.convert(result.getPhonemes(i), british = isBritishVoice(id))
                        chunks.addAll(chunkPhonemes(phonemes))
                    }
                } finally {
                    result.close()
                }
            }
            val sentences = chunks.toTypedArray()
            val pauseCut = normalizePauseCut(prefs.getInt("pause_cut_percent", DEFAULT_PAUSE_CUT_PERCENT))
            val config = GenerationConfig(silenceScale = silenceScaleForCut(pauseCut), sid = id, speed = (request.speechRate / 100.0f).coerceIn(0.5f, 2.0f), phonemeCodepoints = sentences)
            val trimmer = if (prefs.getBoolean("trim_request_silence", false)) RequestBoundaryTrimmer(tts.sampleRate()) else null
            fun writeAudio(samples: FloatArray): Boolean {
                if (ticket != stopGeneration.get()) return false
                val pcm = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    val value = (samples[i].coerceIn(-1f, 1f) * 32767).toInt()
                    pcm[2*i] = value.toByte(); pcm[2*i+1] = (value shr 8).toByte()
                }
                var pos = 0
                while (pos < pcm.size && ticket == stopGeneration.get()) {
                    val n = minOf(callback.maxBufferSize, pcm.size - pos) and -2
                    if (n <= 0 || callback.audioAvailable(pcm, pos, n) != TextToSpeech.SUCCESS) return false
                    pos += n
                }
                return pos == pcm.size && ticket == stopGeneration.get()
            }
            var streamSucceeded = true
            tts.generateWithConfigAndCallback("", config) { samples ->
                if (ticket != stopGeneration.get()) 0 else if (writeAudio(trimmer?.accept(samples) ?: samples)) 1
                else { streamSucceeded = false; 0 }
            }
            if (ticket == stopGeneration.get()) {
                val remaining = trimmer?.finish()
                if (streamSucceeded && (remaining == null || writeAudio(remaining))) callback.done()
                else callback.error()
            }
        } catch (e: Exception) {
            Log.e("ParadeeTTS", "Synthesis failed", e)
            callback.error()
        } catch (e: LinkageError) {
            Log.e("ParadeeTTS", "Misaki native library could not be loaded", e)
            callback.error()
        }
    }
}
