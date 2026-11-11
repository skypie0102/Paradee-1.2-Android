# Paradee 1.2 Android system TTS prototype

An offline Android system TTS engine for arm64-v8a. The app offers system, light, and dark appearance settings. It combines the Paradee 1.2 int8 model with sherpa-onnx 2.0.1, a native Misaki English frontend, and the piper-phonemize 1.5.9 Android library. The engine exposes `af_heart`, `bf_isabella`, `am_puck`, and `bm_fable`; the settings app lets you select and preview a voice. By default the selected voice applies to all requests, including requests with a cached Android default. Enable "Allow other apps to choose a voice" to let clients select one explicitly through Android's TTS API. Speech rate is supported between 0.5 and 2.0. Android 6.0 (API 23) or newer is required.

## Installation

Build as below, install the debug APK on an arm64-v8a phone, open **Paradee TTS**, select a voice and try the preview, then choose it as the preferred engine in the Android text-to-speech settings. To use the selected voice in all applications, leave "Allow other apps to choose a voice" off. A debug APK is signed with the build machine's debug key; it is suitable for testing, not for Play Store release. Android TTS settings and per-app voice selection vary by device. The APK has not been tested on a physical device in this environment.

## Build

Install JDK 17, Android SDK platform 34 and build-tools 34.0.0, then set `ANDROID_HOME` to the SDK directory:

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also builds this debug APK and attaches it as a workflow artifact. The checked-in AARs come from the [sherpa-onnx v2.0.1 release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1) and [piper-phonemize v1.5.9](https://github.com/csukuangfj/piper-phonemize/tree/v1.5.9). The model is from [Paradee-8M-v1.2](https://huggingface.co/sahilmahendrakar/Paradee-8M-v1.2/tree/v1.2).

## How it works and limitations

sherpa-onnx does not provide a native Paradee backend. `convert_model.py` adapts the Paradee ONNX graph to the existing Kokoro backend: the style tensor contains the integer voice ID, which is reshaped and cast into Paradee's `voice` input. The voice table and model metadata make sherpa pass the right ID. The graph and weights otherwise remain unchanged. `verify_model.py` compares both graphs with a fixed random seed and exercises sherpa's phoneme input on the host. To regenerate the bundled assets, run `python3 convert_model.py`.

Paradee's Python package uses Misaki. This app defaults to the pinned `misaki-rs` English port, with US and British lexicons and context-sensitive tagging. Out-of-vocabulary words fall back to eSpeak NG via piper-phonemize and the app's eSpeak-to-Misaki mapping. The APK keeps the English eSpeak dictionary and shared data, and excludes other language dictionaries; English pronunciation is supported, while automatic switches to other languages are not. On upgrade, the app removes obsolete extracted dictionaries from its private storage when the engine starts. The port and fallback may differ from Python Misaki. In **Pronunciation**, choose **eSpeak (previous)** to compare the original frontend offline. The native library and its reproducible build steps are documented in `native/misaki-bridge/NOTICE.md`. Misaki text is split into sentences using heuristics that keep common titles and abbreviations together; ambiguous abbreviations can still be split incorrectly. The pause cut control removes 0–20% of detected quiet pauses of at least 0.2 seconds in 5% steps (default 10%; 0% retains the original; 20% cuts qualifying pauses by one fifth); Paradee has no explicit emotion control, and pitch requests are not implemented. Long phoneme sequences are split before inference. On-device audio quality, phone performance, stop/cancellation behavior, and Android integration still need device testing.

Run Android unit tests with `./gradlew testDebugUnitTest`. After `assembleDebug`, `python3 tools/check_english_espeak.py` verifies the trimmed AAR and APK; adding `--espeak-library /path/to/libespeak-ng.so` compares host English phonemization with the original and trimmed data. Run the native Python-Misaki fixture tests with `cargo test --locked --release --manifest-path native/misaki-bridge/Cargo.toml`, and the host JNI test with `tools/test_misaki_jni.sh`. For model verification install `onnx`, `onnxruntime`, `numpy`, and `sherpa-onnx==2.0.1`, then run `python3 verify_model.py`. This checks the inference path, not Android's text frontend or service.

## App icon

The original parakeet SVG is in `artwork/paradee-parakeet.svg`. Android launcher resources can be regenerated with `python3 tools/generate_launcher_icons.py`; the adaptive icon keeps the bird within the launcher safe zone.

## Licenses

Paradee, Misaki and sherpa-onnx are Apache 2.0; misaki-rs and the spaCy English model are MIT. Notices are in `licenses/` and the Misaki notices are bundled in APK assets. Piper-phonemize's published POM identifies GPLv3 and its source and included components have their own license notices; consult [its license file](https://github.com/csukuangfj/piper-phonemize/blob/v1.5.9/LICENSE.md) and [license directory](https://github.com/csukuangfj/piper-phonemize/tree/v1.5.9/licenses) before distributing a derivative. The Gradle dependencies have additional licenses.
