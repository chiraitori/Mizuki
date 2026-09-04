plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.chiraitori.mizuki"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.chiraitori.mizuki"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

    // FFmpeg + Python + aria2c are roughly 50 MB per architecture.
    // We build per-ABI splits (arm64-v8a, armeabi-v7a, x86, x86_64) plus a universal APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    val keystorePath = System.getenv("KEYSTORE_FILE")
    val keystoreFile = if (!keystorePath.isNullOrEmpty()) file(keystorePath) else rootProject.file("release.keystore")

    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("KEY_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: System.getenv("ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            } else {
                // Graceful fallback to debug signing if no release key is configured yet
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("compact") {
            initWith(getByName("release"))
            // Installable local build with the same optimizations as release.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val name = output.filters
                    .find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                    ?.identifier
                val baseAbiCode = abiCodes[name] ?: 0
                output.versionCode.set(baseAbiCode + (output.versionCode.get() ?: 1))
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "lib/**/libc++_shared.so"
            pickFirsts += "lib/**/libcrypto.so"
            pickFirsts += "lib/**/libssl.so"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose UI & Foundation & Animation
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    // Material 3 & Material 3 Expressive
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)

    // Material 3 Adaptive
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Coil for Thumbnail Loading
    implementation(libs.coil.compose)

    // yt-dlp Core Engine & FFmpeg & Aria2c (Seal's core backend)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    implementation(libs.youtubedl.aria2c)

    // DataStore for Preferences
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
