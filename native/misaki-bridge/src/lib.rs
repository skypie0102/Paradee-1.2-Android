use jni::objects::{GlobalRef, JObject, JString, JValue};
use jni::sys::{jboolean, jlong, jstring};
use jni::{JNIEnv, JavaVM};
use misaki_rs::{fallback::FallbackError, Fallback, G2P, Language};
use std::panic::{catch_unwind, AssertUnwindSafe};

/// Calls the app's existing offline eSpeak frontend only for unknown words.
struct AndroidFallback {
    vm: JavaVM,
    bridge: GlobalRef,
    british: bool,
}

impl Fallback for AndroidFallback {
    fn phonemize(&self, word: &str) -> Result<String, FallbackError> {
        let fail = |message: String| FallbackError::Espeak {
            word: word.to_owned(),
            error: message,
        };
        let mut env = self.vm.attach_current_thread().map_err(|e| fail(e.to_string()))?;
        let input = env.new_string(word).map_err(|e| fail(e.to_string()))?;
        let input_obj = JObject::from(input);
        let result = env.call_method(
            self.bridge.as_obj(), "fallbackWord", "(Ljava/lang/String;Z)Ljava/lang/String;",
            &[JValue::Object(&input_obj), JValue::Bool(self.british as jboolean)],
        );
        let object = match result {
            Ok(value) => value.l().map_err(|e| fail(e.to_string()))?,
            Err(e) => {
                if env.exception_check().unwrap_or(false) { let _ = env.exception_clear(); }
                return Err(fail(e.to_string()));
            }
        };
        if object.is_null() { return Err(fail("phonemizer returned null".into())); }
        let output = JString::from(object);
        let phonemes: String = env.get_string(&output).map_err(|e| fail(e.to_string()))?.into();
        if phonemes.is_empty() { return Err(fail("phonemizer returned an empty result".into())); }
        Ok(phonemes)
    }
}

fn throw(env: &mut JNIEnv<'_>, message: &str) {
    if env.exception_check().unwrap_or(false) { let _ = env.exception_clear(); }
    let _ = env.throw_new("java/lang/IllegalStateException", message);
}

// Handles are owned by Kotlin and accessed only while holding its synchronized lock.
#[no_mangle]
pub extern "system" fn Java_org_example_paradeetts_MisakiBridge_nativeCreate(
    mut env: JNIEnv<'_>, object: JObject<'_>, british: jboolean,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| -> Result<jlong, String> {
        let fallback = AndroidFallback {
            vm: env.get_java_vm().map_err(|e| e.to_string())?,
            bridge: env.new_global_ref(object).map_err(|e| e.to_string())?,
            british: british != 0,
        };
        let lang = if british != 0 { Language::EnglishGB } else { Language::EnglishUS };
        let mut g2p = G2P::with_fallback(lang, Some(Box::new(fallback)));
        g2p.unk.clear(); // Match Paradee's original Misaki configuration.
        Ok(Box::into_raw(Box::new(g2p)) as jlong)
    }));
    match result {
        Ok(Ok(handle)) => handle,
        Ok(Err(message)) => { throw(&mut env, &message); 0 }
        Err(_) => { throw(&mut env, "Could not initialize Misaki"); 0 }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_example_paradeetts_MisakiBridge_nativePhonemize(
    mut env: JNIEnv<'_>, _object: JObject<'_>, handle: jlong, text: JString<'_>,
) -> jstring {
    let result = catch_unwind(AssertUnwindSafe(|| -> Result<String, String> {
        if handle == 0 { return Err("Misaki is not initialized".into()); }
        let text: String = env.get_string(&text).map_err(|e| e.to_string())?.into();
        // SAFETY: handle comes from nativeCreate and Kotlin serializes access/release.
        let engine = unsafe { &*(handle as *const G2P) };
        engine.g2p(&text).map(|(phonemes, _)| phonemes).map_err(|e| e.to_string())
    }));
    match result {
        Ok(Ok(value)) => match env.new_string(value) {
            Ok(value) => value.into_raw(),
            Err(e) => { throw(&mut env, &e.to_string()); std::ptr::null_mut() }
        },
        Ok(Err(message)) => { throw(&mut env, &message); std::ptr::null_mut() }
        Err(_) => { throw(&mut env, "Misaki phonemization failed"); std::ptr::null_mut() }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_example_paradeetts_MisakiBridge_nativeRelease(
    _env: JNIEnv<'_>, _object: JObject<'_>, handle: jlong,
) {
    if handle != 0 {
        // SAFETY: Kotlin releases each created handle exactly once, under its lock.
        unsafe { drop(Box::from_raw(handle as *mut G2P)); }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn context_changes_heteronym_pronunciation() {
        let mut g2p = G2P::with_fallback(Language::EnglishUS, None);
        g2p.unk.clear();
        assert_eq!(g2p.g2p("Please read the report I read yesterday.").unwrap().0,
                   "plˈiz ɹˈid ðə ɹəpˈɔɹt ˌI ɹˈɛd jˈɛstəɹdˌA.");
        assert_eq!(g2p.g2p("They record a record every week.").unwrap().0,
                   "ðˌA ɹəkˈɔɹd ɐ ɹˈɛkəɹd ˈɛvəɹi wˈik.");
    }

    #[test]
    fn british_and_american_use_their_own_lexicons() {
        let us = G2P::with_fallback(Language::EnglishUS, None);
        let gb = G2P::with_fallback(Language::EnglishGB, None);
        let a = us.g2p("bath").unwrap().0;
        let b = gb.g2p("bath").unwrap().0;
        assert_ne!(a, b);
        assert!(a.contains('æ'), "American bath: {a}");
        assert!(b.contains('ɑ'), "British bath: {b}");
        assert_eq!(us.g2p("The bath is near the water.").unwrap().0,
                   "ðə bˈæθ ɪz nˌɪɹ ðə wˈɔTəɹ.");
        assert_eq!(gb.g2p("The bath is near the water.").unwrap().0,
                   "ðə bˈɑːθ ɪz nˌɪə ðə wˈɔːtə.");
        assert_eq!(us.g2p("Never, potter, poured.").unwrap().0,
                   "nˈɛvəɹ, pˈɑTəɹ, pˈɔɹd.");
        assert_eq!(gb.g2p("Never, potter, poured.").unwrap().0,
                   "nˈɛvə, pˈɒtə, pˈɔːd.");
        assert_eq!(us.g2p("Testing $123.45 on November 2nd.").unwrap().0,
                   "tˈɛstɪŋ wˈʌn hˈʌndɹəd twˈɛnti θɹˈi dˈɑləɹz ænd fˈɔɹTi fˈIv sˈɛnts ˌɔn nOvˈɛmbəɹ sˈɛkənd.");
        assert_eq!(gb.g2p("Testing $123.45 on November 2nd.").unwrap().0,
                   "tˈɛstɪŋ wˈʌn hˈʌndɹəd twˈɛnti θɹˈiː dˈɒləz and fˈɔːti fˈIv sˈɛnts ˌɒn nQvˈɛmbə sˈɛkᵊnd.");
    }
}
