package org.example.paradeetts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private var previewTts: TextToSpeech? = null
    private var previewReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("paradee", MODE_PRIVATE)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        layout.addView(TextView(this).apply { text = "Paradee 1.2 system TTS"; textSize = 22f })
        layout.addView(TextView(this).apply { text = "Voice used in other apps:"; setPadding(0, 24, 0, 12) })
        layout.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, voiceLabels)
            setSelection(prefs.getInt("voice", 0).coerceIn(0, 3))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { prefs.edit().putInt("voice", position).apply() }
            }
        })
        layout.addView(Switch(this).apply {
            text = "Allow other apps to choose a voice"
            isChecked = prefs.getBoolean("allow_app_voices", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("allow_app_voices", checked).apply() }
        })
        layout.addView(TextView(this).apply {
            text = "Leave this off to use the selected voice in every app. When on, apps can override it."
            setPadding(0, 8, 0, 12)
        })
        val sampleText = EditText(this).apply {
            setText("Hello, I am speaking with the selected Paradee voice.")
            hint = "Text to preview"
            setSingleLine(false)
        }
        layout.addView(sampleText)
        val preview = Button(this).apply {
            text = "Preparing voice preview…"
            isEnabled = false
            setOnClickListener {
                val engine = previewTts ?: return@setOnClickListener
                val id = prefs.getInt("voice", 0).coerceIn(0, 3)
                val voice = engine.voices?.firstOrNull { it.name == voiceNames[id] }
                if (!previewReady || voice == null || engine.setVoice(voice) != TextToSpeech.SUCCESS) {
                    Toast.makeText(this@MainActivity, "Could not select the preview voice", Toast.LENGTH_LONG).show()
                } else if (engine.speak(sampleText.text.toString(), TextToSpeech.QUEUE_FLUSH, null, "paradee-preview") != TextToSpeech.SUCCESS) {
                    Toast.makeText(this@MainActivity, "Could not start the preview", Toast.LENGTH_LONG).show()
                }
            }
        }
        layout.addView(preview)
        layout.addView(Button(this).apply { text = "Open text-to-speech settings"; setOnClickListener { startActivity(Intent("com.android.settings.TTS_SETTINGS")) } })
        layout.addView(TextView(this).apply { text = "Choose Paradee TTS as the preferred engine in Android settings. Speech works offline."; setPadding(0, 24, 0, 0) })
        setContentView(layout, ViewGroup.LayoutParams(-1, -1))

        previewTts = TextToSpeech(this, { status ->
            previewReady = status == TextToSpeech.SUCCESS
            preview.isEnabled = previewReady
            preview.text = if (previewReady) "Play selected voice" else "Voice preview unavailable"
        }, packageName)
    }

    override fun onDestroy() {
        previewTts?.stop()
        previewTts?.shutdown()
        previewTts = null
        super.onDestroy()
    }
}
class CheckVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = Intent().putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, arrayListOf("eng-USA", "eng-GBR"))
            .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf())
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, data); finish()
    }
}
class GetSampleText : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(TextToSpeech.LANG_AVAILABLE, Intent().putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, "Hello! This is Paradee speaking on your device.")); finish()
    }
}
