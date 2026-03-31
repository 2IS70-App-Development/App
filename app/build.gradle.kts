plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sonar)
    jacoco
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
            enableAndroidTestCoverage = true
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
}

sonar {
    properties {
        property("sonar.projectName", "CryptoSeal")
        property("sonar.projectKey", "2IS70-App-Development_App")
        property("sonar.organization", "2is70-app-development")
        property("sonar.host.url", "https://sonarcloud.io")
        
        // Point to the JaCoCo XML report (Relative to app folder)
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.buildDir}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        
        // Exclude UI from analysis
        property("sonar.exclusions", "**/R.java, **/BuildConfig.java, **/*Activity*.kt, **/*Tab*.kt, **/*Screen*.kt, **/*Composable*.kt, **/*Preview*.kt, **/Theme*.kt, **/Navigation*.kt")
    }
}

val fileFilter = mutableSetOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*Activity*.*",
    "**/*TabKt*.*",
    "**/*ScreenKt*.*",
    "**/*ComposableKt*.*",
    "**/*PreviewKt*.*",
    "**/ThemeKt*.*",
    "**/NavigationKt*.*",
    "**/*Application*.*"
)

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

project.afterEvaluate {
    tasks.register<JacocoReport>("jacocoTestReport") {
        dependsOn("testDebugUnitTest")
        group = "Reporting"
        description = "Generate Jacoco coverage reports"

        reports {
            xml.required.set(true)
            html.required.set(true)
            xml.outputLocation.set(file("${project.buildDir}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
            html.outputLocation.set(file("${project.buildDir}/reports/jacoco/jacocoTestReport/html"))
        }

        val javaClasses = fileTree("${project.buildDir}/intermediates/javac/debug") {
            include("**/*.class")
            exclude(fileFilter)
        }
        val kotlinClasses = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
            include("**/*.class")
            exclude(fileFilter)
        }
        classDirectories.setFrom(files(javaClasses, kotlinClasses))

        sourceDirectories.setFrom(files(
            "${project.projectDir}/src/main/java",
            "${project.projectDir}/src/main/kotlin"
        ))

        executionData.setFrom(fileTree(project.buildDir) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/*coverage.ec"
            )
        })
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

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
    implementation("androidx.camera:camera-core:1.5.3")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.security:security-crypto:1.1.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
