import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.android")
    id("jacoco")
}


jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    namespace = "app.cryptoseal"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.cryptoseal"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/AL2.0"
            excludes += "/META-INF/LGPL2.1"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Ensure Kotlin JVM target matches Java compile options (11)
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.gson)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.barcode.scanning)
    implementation(libs.play.services.location)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/androidx/**/*.*",
        "**/*\$Companion*.*",
        "**/*ComposableSingletons*.*"
    )

    val buildDirFile = layout.buildDirectory.get().asFile

    val kotlinClasses = fileTree(buildDirFile.resolve("tmp/kotlin-classes/debug")) {
        exclude(excludes)
    }

    val javaClasses = fileTree(buildDirFile.resolve("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude(excludes)
    }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    // sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(
        fileTree(buildDirFile) {
            include(
                "**/*.exec",
                "**/*.ec"
            )
        }
    )
}

tasks.register("printUnitTestInfo") {
    doLast {
        println("build dir = ${layout.buildDirectory.get().asFile}")

        val candidates = fileTree(layout.buildDirectory.get().asFile) {
            include("**/*.exec", "**/*.ec", "**/TEST-*.xml")
        }.files.sortedBy { it.absolutePath }

        if (candidates.isEmpty()) {
            println("No .exec/.ec/TEST-*.xml files found under build/")
        } else {
            candidates.forEach { println(it.absolutePath) }
        }
    }
}