plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.theolm.bydapps.gecko.ytmusic"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.theolm.bydapps.gecko.ytmusic"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-gecko"

        ndk {
            abiFilters += "arm64-v8a"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":gecko-core"))
}
