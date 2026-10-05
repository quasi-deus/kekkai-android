plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.quasideus.kekkai"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.quasideus.kekkai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Git sync for the password store, in-app (no Termux dependency).
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.1.202505142326-r")

    // OpenKeychain Remote OpenPGP API integration (Kotlin reimplementation of
    // OpenKeychain's own openpgp-api, maintained by the Android Password Store
    // project). See repository/pass/OpenPgpDecryptor.kt for a vendored-fallback
    // note if this JitPack coordinate stops resolving.
    implementation("com.github.android-password-store:openpgp-ktx:3.0.0")
}
