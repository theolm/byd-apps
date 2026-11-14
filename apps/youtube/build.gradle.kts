plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystorePath = System.getenv("KEYSTORE_PATH") ?: "$rootDir/keystore/release.keystore"
val keystoreFile = file(keystorePath)

android {
    namespace = "dev.theolm.bydapps.youtube"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.theolm.bydapps.youtube"
        minSdk = 29
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as String?)?.toInt()
            ?: libs.versions.versionCode.get().toInt()
        versionName = (project.findProperty("versionName") as String?)
            ?: libs.versions.versionName.get()

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(project(":browser-core"))
}
