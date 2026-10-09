import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

android {
    namespace = "com.github.carlosliszt.plantsiot"
    compileSdk {
        version = release(36)
    }



    defaultConfig {
        applicationId = "com.github.carlosliszt.plantsiot"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"


        val apiKey = localProperties.getProperty("TREFLE_API_TOKEN") ?: ""

        buildConfigField("String", "MQTT_BROKER", "\"ssl://3324ab5a5cd44751b6c3aacc57b60320.s1.eu.hivemq.cloud:8883\"")
        buildConfigField("String", "MQTT_USER", "\"alice\"")
        buildConfigField("String", "MQTT_PASS", "\"123456789\"")
        buildConfigField("String", "TREFLE_API_TOKEN", "\"${apiKey}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
