plugins {
    // Android Application plugin for building an Android app.
    alias(libs.plugins.android.application)
    // Kotlin plugin with Compose support for modern UI development.
    alias(libs.plugins.kotlin.compose)
}

android {
    // Unique identifier for the package.
    namespace = "app.cryptoseal"
    // The SDK version used for compiling the code.
    compileSdk = 36

    defaultConfig {
        // Unique application ID for the Play Store.
        applicationId = "app.cryptoseal"
        // Minimum Android version supported (Android 7.0).
        minSdk = 24
        // Target Android version for runtime behavior (Android 15+).
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Specifies the instrumentation test runner.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Disable code shrinking/obfuscation for now.
            isMinifyEnabled = false
            // Default ProGuard/R8 rules.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Set Java compatibility to version 11.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enable support for modern Java 8+ APIs on older Android versions.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        // Enable Jetpack Compose support.
        compose = true
    }
}

dependencies {
    // Core Android KTX extensions for Kotlin.
    implementation(libs.androidx.core.ktx)
    // Lifecycle components for ViewModel and coroutine integration.
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Activity integration for Jetpack Compose.
    implementation(libs.androidx.activity.compose)
    // BOM (Bill of Materials) ensures consistent versions across Compose libraries.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Material 3 UI components.
    implementation(libs.androidx.compose.material3)

    // Unit testing dependency.
    testImplementation(libs.junit)
    // Android instrumentation testing dependencies.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Debug tools for Compose.
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation-Compose: Handles screen transitions and routing in the app.
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // ViewModel-Compose: Integration for using ViewModels within Composable functions.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // CameraX: Modern Android camera library for taking photos and image analysis.
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
    implementation("androidx.camera:camera-core:1.5.3")

    // Google ML Kit: Used specifically for real-time QR code/barcode detection.
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Play Services Location: Used for capturing GPS coordinates during package scans.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Extended Material Icons: Provides a wider variety of vector icons for the UI.
    implementation("androidx.compose.material:material-icons-extended")

    // Gson: Google's library for JSON serialization and deserialization.
    implementation("com.google.code.gson:gson:2.10.1")

    // ZXing: "Zebra Crossing" library used for generating QR code bitmaps.
    implementation("com.google.zxing:core:3.5.3")

    // Jetpack Security: EncryptedSharedPreferences for secure local data storage.
    implementation("androidx.security:security-crypto:1.1.0")

    // Core library desugaring for Java 8+ API support on older devices.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
