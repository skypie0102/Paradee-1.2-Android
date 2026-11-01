package org.example.paradeetts

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private var previewTts: TextToSpeech? = null
    private var previewReady = false
    private var sampleInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("paradee", MODE_PRIVATE)
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = useDarkTheme(prefs.getString("theme_mode", "system"), systemDark)
        setTheme(if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        layout.addView(TextView(this).apply { text = "Paradee 1.2 system TTS"; textSize = 22f })
        layout.addView(TextView(this).apply { text = "Appearance:"; setPadding(0, 24, 0, 12) })
        layout.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, themeLabels)
            val savedMode = prefs.getString("theme_mode", "system")
            setSelection(themeModes.indexOf(savedMode).takeIf { it >= 0 } ?: 0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val mode = themeModes[position]
                    if (mode != prefs.getString("theme_mode", "system")) {
                        prefs.edit().putString("theme_mode", mode).apply()
                        recreate()
                    }
                }
            }
        })
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
        val pauseValue = prefs.getInt("pause_cut_percent", DEFAULT_PAUSE_CUT_PERCENT).coerceIn(0, 100)
        val pauseLabel = TextView(this).apply {
            text = "Cut quiet pauses: $pauseValue%"
            setPadding(0, 24, 0, 4)
        }
        layout.addView(pauseLabel)
        val pauseSlider = SeekBar(this).apply {
            max = 100
            progress = pauseValue
            contentDescription = "Percentage to cut from quiet pauses"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    pauseLabel.text = "Cut quiet pauses: $value%"
                    prefs.edit().putInt("pause_cut_percent", value).apply()
                }
            })
        }
        layout.addView(pauseSlider)
        layout.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val buttonLayout = { LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
            addView(Button(this@MainActivity).apply {
                text = "− 1%"
                contentDescription = "Reduce pause cut by one percent"
                setOnClickListener { pauseSlider.progress = (pauseSlider.progress - 1).coerceAtLeast(0) }
            }, buttonLayout())
            addView(Button(this@MainActivity).apply {
                text = "+ 1%"
                contentDescription = "Increase pause cut by one percent"
                setOnClickListener { pauseSlider.progress = (pauseSlider.progress + 1).coerceAtMost(100) }
            }, buttonLayout())
        })
        layout.addView(TextView(this).apply {
            text = "0% keeps the original; 100% removes detected quiet pauses. Applies to quiet stretches of at least 0.2 seconds."
            setPadding(0, 8, 0, 12)
        })
        val sampleText = EditText(this).apply {
            setText(savedInstanceState?.getString("preview_text") ?: "Hello, I am speaking with the selected Paradee voice.")
            hint = "Text to preview"
            setSingleLine(false)
        }
        sampleInput = sampleText
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
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(layout) }
        setContentView(scroll, ViewGroup.LayoutParams(-1, -1))

        previewTts = TextToSpeech(this, { status ->
            previewReady = status == TextToSpeech.SUCCESS
            preview.isEnabled = previewReady
            preview.text = if (previewReady) "Play selected voice" else "Voice preview unavailable"
        }, packageName)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("preview_text", sampleInput?.text?.toString())
        super.onSaveInstanceState(outState)
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
