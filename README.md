# Paradee 1.2 Android system TTS prototype

An offline Android system TTS engine for arm64-v8a. It combines the Paradee 1.2 int8 model with sherpa-onnx 2.0.1 and the piper-phonemize 1.5.9 Android library. The engine exposes `af_heart`, `bf_isabella`, `am_puck`, and `bm_fable`; the settings app also lets you select a default voice. Apps may choose a voice explicitly through Android's TTS API. Speech rate is supported between 0.5 and 2.0. Android 6.0 (API 23) or newer is required.

## Installation

Build as below, install the debug APK on an arm64-v8a phone, open **Paradee TTS**, and choose it as the preferred engine in the Android text-to-speech settings. A debug APK is signed with the build machine's debug key; it is suitable for testing, not for Play Store release. Android TTS settings and per-app voice selection vary by device. The APK has not been tested on a physical device in this environment.

## Build

Install JDK 17, Android SDK platform 34 and build-tools 34.0.0, then set `ANDROID_HOME` to the SDK directory:

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also builds this debug APK and attaches it as a workflow artifact. The checked-in AARs come from the [sherpa-onnx v2.0.1 release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1) and [piper-phonemize v1.5.9](https://github.com/csukuangfj/piper-phonemize/tree/v1.5.9). The model is from [Paradee-8M-v1.2](https://huggingface.co/sahilmahendrakar/Paradee-8M-v1.2/tree/v1.2).

## How it works and limitations

sherpa-onnx does not provide a native Paradee backend. `convert_model.py` adapts the Paradee ONNX graph to the existing Kokoro backend: the style tensor contains the integer voice ID, which is reshaped and cast into Paradee's `voice` input. The voice table and model metadata make sherpa pass the right ID. The graph and weights otherwise remain unchanged. `verify_model.py` compares both graphs with a fixed random seed and exercises sherpa's phoneme input on the host. To regenerate the bundled assets, run `python3 convert_model.py`.

The original Paradee frontend uses Misaki; this prototype uses eSpeak NG via piper-phonemize, with American or British English according to the voice. Phoneme choices, punctuation, numbers, and pronunciations can differ from the original. Pitch requests are not implemented. Long phoneme sequences are split before inference. On-device audio quality, phone performance, stop/cancellation behavior, and Android integration still need device testing.

For host verification install `onnx`, `onnxruntime`, `numpy`, and `sherpa-onnx==2.0.1`, then run `python3 verify_model.py`. This checks the inference path, not Android's text frontend or service.

## Licenses

Paradee and sherpa-onnx are Apache 2.0; see `licenses/`. Piper-phonemize's published POM identifies GPLv3 and its source and included components have their own license notices; consult [its license file](https://github.com/csukuangfj/piper-phonemize/blob/v1.5.9/LICENSE.md) and [license directory](https://github.com/csukuangfj/piper-phonemize/tree/v1.5.9/licenses) before distributing a derivative. The Gradle dependencies have additional licenses.
