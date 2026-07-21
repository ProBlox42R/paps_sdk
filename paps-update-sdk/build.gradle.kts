plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.paps.updatesdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Deliberately minimal - just enough to make one GET request and show
    // a dialog, so adding this to an existing app doesn't drag in Compose,
    // Retrofit, or anything else that app might not already use.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
