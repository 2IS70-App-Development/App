// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sonar) apply false
}

subprojects {
    apply(plugin = "org.sonarqube")

    configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            property("sonar.projectName", "App")
            property("sonar.projectKey", "app.cryptoseal")
            property("sonar.organization", "2is70-app-development")
            property("sonar.host.url", "https://sonarcloud.io")

            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
            )
            property(
                "sonar.exclusions",
                "**/R.java, **/BuildConfig.java, **/*Activity*.kt, **/*Tab*.kt, **/*Screen*.kt, **/*Composable*.kt, **/*Preview*.kt, **/Theme*.kt, **/Navigation*.kt"
            )
        }
    }
}
