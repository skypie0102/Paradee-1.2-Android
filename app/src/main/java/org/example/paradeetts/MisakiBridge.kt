package org.example.paradeetts

import com.github.csukuangfj.piper.phonemize.piperPhonemizeText

/** Offline native Misaki port; eSpeak is used for words outside its lexicons. */
internal object MisakiBridge {
    private var americanHandle = 0L
    private var britishHandle = 0L

    init {
        System.loadLibrary("paradee_misaki")
    }

    @Synchronized
    fun phonemize(text: String, british: Boolean): String {
        val handle = if (british) {
            if (britishHandle == 0L) britishHandle = nativeCreate(true)
            britishHandle
        } else {
            if (americanHandle == 0L) americanHandle = nativeCreate(false)
            americanHandle
        }
        check(handle != 0L) { "Misaki failed to initialize" }
        return nativePhonemize(handle, text)
    }

    /** JNI callback: preserve the app's established offline OOV fallback. */
    @Suppress("unused")
    @JvmName("fallbackWord")
    fun fallbackWord(word: String, british: Boolean): String {
        val result = piperPhonemizeText(word, if (british) "en-gb-x-rp" else "en-us")
            ?: throw IllegalStateException("eSpeak could not phonemize an unfamiliar word")
        return try {
            (0 until result.numSentences).mapNotNull { i ->
                val codepoints = ParadeePhonemes.convert(result.getPhonemes(i), british)
                if (codepoints.isEmpty()) null else String(codepoints, 0, codepoints.size).trim()
            }.filter { it.isNotEmpty() }.joinToString(" ")
        } finally {
            result.close()
        }
    }

    @Synchronized
    fun release() {
        if (americanHandle != 0L) nativeRelease(americanHandle)
        if (britishHandle != 0L) nativeRelease(britishHandle)
        americanHandle = 0L
        britishHandle = 0L
    }

    private external fun nativeCreate(british: Boolean): Long
    private external fun nativePhonemize(handle: Long, text: String): String
    private external fun nativeRelease(handle: Long)
}
