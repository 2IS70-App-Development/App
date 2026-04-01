// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.sonarqube") version "7.1.0.6387"
}

sonar {
    properties {
        property(
            "sonar.projectKey",
            "2IS70-App-Development_App"
        ) // Unique project identifier in SonarQube
        property(
            "sonar.organization",
            "2is70-app-development"
        ) // Display name shown in SonarQube UI
        property("sonar.host.url", "https://sonarcloud.io") // SonarQube server URL
        property("sonar.token", "1f5a057acb8a6578795596b112cc23789f53cb82") // Authentication token
//       property(
//           "sonar.sources",
//           "app/src/main/java,app/src/main/kotlin"
//       ) // Application source code paths
//       property("sonar.tests", "app/src/test/java,app/src/test/kotlin") // Unit test source paths
//       property(
//           "sonar.test.inclusions",
//           "**/*Test*.kt, **/*Test*.java"
//       )  // Identifies test classes
//       property(
//           "sonar.java.binaries",
//           "app/build/tmp/kotlin-classes/debug,app/build/intermediates/javac/debug"
//       ) // Compiled bytecode required for static analysis
    //    property(
    //        "sonar.junit.reportPaths",
    //        "app/build/test-results/testDebugUnitTest"
    //    )  // JUnit test execution reports
//       property(
//           "sonar.exclusions",
//           "**/R.class, **/R$*.class, **/BuildConfig.*, **/Manifest*.*, build/**"
//       )  // Exclude generated and irrelevant files

        property(
            "sonar.scm.disabled",
            "true"
        ) // Temporarily disable SCM detection to avoid Git autodetection error

        //Integrate JacoCo to SonarQube
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${rootProject.projectDir}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
    }
}