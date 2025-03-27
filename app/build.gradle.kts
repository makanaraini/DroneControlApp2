plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dronecontrolapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dronecontrolapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}
dependencies {
    implementation(libs.play.services.maps) // Google Maps SDK
    implementation("androidx.compose.material:material:1.7.8") // Material Design
    implementation(libs.retrofit) // Retrofit for API calls
    implementation(libs.socket.io) // Socket.IO for real-time communication
    implementation(libs.material3) // Material 3
    implementation(libs.ui) // Compose UI
    implementation(libs.androidx.foundation) // Compose Foundation
    implementation(libs.androidx.activity.compose.v180) // Compose Activity
    implementation(libs.androidx.core.ktx.v160) // Ensure you have this for NotificationCompat
    implementation(libs.androidx.appcompat) // Required for backward compatibility
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.osmdroid.android) // OSMdroid for map support
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}