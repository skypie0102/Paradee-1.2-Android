plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

// Keep the original AAR in source control, but package only its English dictionary.
// The small voice configurations and shared phoneme data are retained intact.
val englishPiper = tasks.register<Zip>("prepareEnglishPiper") {
    from(zipTree(layout.projectDirectory.file("libs/piper-phonemize-v1.5.9.aar"))) {
        exclude {
            val prefix = "assets/espeak-ng-data/"
            val relative = it.path.removePrefix(prefix)
            it.path.startsWith(prefix) && !relative.contains('/') &&
                relative.endsWith("_dict") && relative != "en_dict"
        }
    }
    archiveFileName.set("piper-phonemize-english.aar")
    destinationDirectory.set(layout.buildDirectory.dir("generated/piper"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
android {
    namespace = "org.example.paradeetts"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.example.paradeetts"
        minSdk = 23
        targetSdk = 34
        versionCode = 9
        versionName = "1.8"
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(files("libs/sherpa-onnx-2.0.1.aar"))
    implementation(files(englishPiper))
    testImplementation("junit:junit:4.13.2")
}
