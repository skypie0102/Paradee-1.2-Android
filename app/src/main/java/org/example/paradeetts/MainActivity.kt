package org.example.paradeetts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*

val voiceNames = listOf("af_heart", "bf_isabella", "am_puck", "bm_fable")

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("paradee", MODE_PRIVATE)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        layout.addView(TextView(this).apply { text = "Paradee 1.2 system TTS"; textSize = 22f })
        layout.addView(TextView(this).apply { text = "Default voice (apps may request a specific voice):"; setPadding(0, 24, 0, 12) })
        layout.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, voiceNames)
            setSelection(prefs.getInt("voice", 0).coerceIn(0, 3))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { prefs.edit().putInt("voice", position).apply() }
            }
        })
        layout.addView(Button(this).apply { text = "Open text-to-speech settings"; setOnClickListener { startActivity(Intent("com.android.settings.TTS_SETTINGS")) } })
        layout.addView(TextView(this).apply { text = "Choose Paradee TTS as the preferred engine in Android settings. The model and speech processing work offline."; setPadding(0, 24, 0, 0) })
        setContentView(layout, ViewGroup.LayoutParams(-1, -1))
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
