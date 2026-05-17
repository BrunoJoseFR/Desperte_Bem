plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {

    namespace = "com.example.despertebem"
    compileSdk = 35

    defaultConfig {

        applicationId = "com.example.despertebem"

        minSdk = 31
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
}