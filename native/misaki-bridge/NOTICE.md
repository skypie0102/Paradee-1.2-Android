# Misaki Android bridge

The bridge uses the MIT-licensed [misaki-rs](https://github.com/MicheleYin/misaki-rs) at commit `5a6ca6ccc5a234f583cbcac2e1c25b16c33405e5`, with its default eSpeak feature disabled. It embeds the port's US/GB English pronunciation data and the POS tagger model. The implementation derives from Apache-2.0 [Misaki](https://github.com/hexgrad/misaki). The POS model is derived from the MIT-licensed spaCy `en_core_web_sm` model; its license and source attribution are in `licenses/` and in the APK assets. The MIT Rust port's notice is in `licenses/misaki-rs-MIT.txt`.

The JNI callback uses the app's existing piper-phonemize/eSpeak frontend only for words missing from Misaki's lexicons. Its fallback mapping is an approximation and is not expected to match every Python fallback pronunciation exactly. The Rust port is not the Python/spaCy code; it aims to reproduce that frontend's behavior, which can diverge on inputs outside the fixtures.

The US fixture is copied from the pinned misaki-rs project's MIT-licensed `tests/fixtures/golden_adversarial_us.json`. The British fixture was generated with Misaki 0.9.4 (commit `fba1236595f2d2bf21d414ba6e57d25256afada3`), spaCy 3.8.18, and `en_core_web_sm` 3.8.0, with fallback disabled and `unk=""`. Tests compare the resulting phoneme strings against both fixture sets; the host JNI test checks fallback callback routing separately.

To rebuild for arm64, install Rust with the `aarch64-linux-android` target and Android NDK 27.2, set `ANDROID_NDK_HOME`, and run `tools/build_misaki_android.sh`. Android API 23 is the native library's minimum. `tools/test_misaki_jni.sh` builds a host library and exercises JNI, including an unknown-word callback.
