package org.example.paradeetts;

/** Exercises the same JNI entry points and callbacks as the Android class. */
public final class MisakiBridge {
    private native long nativeCreate(boolean british);
    private native String nativePhonemize(long handle, String text);
    private native void nativeRelease(long handle);
    private int fallbackCalls;

    public String fallbackWord(String word, boolean british) {
        if (!word.equalsIgnoreCase("quuxflorp")) throw new IllegalArgumentException("Unexpected OOV word");
        fallbackCalls++;
        return british ? "kwˈʌksflɔːp" : "kwˈʌksflɔɹp";
    }

    private static void equal(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but got " + actual);
    }

    public static void main(String[] args) {
        System.loadLibrary("paradee_misaki");
        MisakiBridge bridge = new MisakiBridge();
        long us = bridge.nativeCreate(false);
        long gb = bridge.nativeCreate(true);
        if (us == 0 || gb == 0) throw new AssertionError("native initialization failed");
        try {
            equal("plˈiz ɹˈid ðə ɹəpˈɔɹt ˌI ɹˈɛd jˈɛstəɹdˌA.",
                bridge.nativePhonemize(us, "Please read the report I read yesterday."));
            equal("ðə bˈɑːθ ɪz nˌɪə ðə wˈɔːtə.",
                bridge.nativePhonemize(gb, "The bath is near the water."));
            equal("wˌɛɹ ɪz mI kwˈʌksflɔɹp?", bridge.nativePhonemize(us, "Where is my quuxflorp?"));
            equal("wˌɛː ɪz mI kwˈʌksflɔːp?", bridge.nativePhonemize(gb, "Where is my quuxflorp?"));
            if (bridge.fallbackCalls < 2) throw new AssertionError("OOV callbacks were not made");
            System.out.println("JNI US/GB output, callbacks and release passed");
        } finally {
            bridge.nativeRelease(us);
            bridge.nativeRelease(gb);
        }
    }
}
