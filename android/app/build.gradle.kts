plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.matchering.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.matchering.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Chaquopy provides native wheels for these ABIs
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

python {
    version = "3.11"

    pip {
        // Core scientific stack — Chaquopy provides pre-built wheels for these
        install("numpy")
        install("scipy")
        install("resampy")
        // statsmodels is heavy and may fail to install on some setups;
        // the Python bridge (mg_android.py) provides a fallback LOWESS
        // implementation if this import fails at runtime.
        install("statsmodels")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// Copy the matchering Python library from the repo root into the Android
// project's Python source directory so Chaquopy bundles it in the APK.
tasks.register<Copy>("copyMatchering") {
    from("../../matchering")
    into("src/main/python/matchering")
}

tasks.named("preBuild") {
    dependsOn("copyMatchering")
}
