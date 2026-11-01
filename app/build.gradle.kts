plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "org.example.paradeetts"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.example.paradeetts"
        minSdk = 23
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(files("libs/sherpa-onnx-2.0.1.aar"))
    implementation(files("libs/piper-phonemize-v1.5.9.aar"))
    testImplementation("junit:junit:4.13.2")
}
