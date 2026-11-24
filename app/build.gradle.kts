plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.eventkampus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.eventkampus"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Keystore akan dibuat di Step 2
            // Isi nilai-nilai ini setelah keystore dibuat
            storeFile = file("eventkampus-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "eventkampus2026"
            keyAlias = System.getenv("KEY_ALIAS") ?: "eventkampus"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "eventkampus2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
