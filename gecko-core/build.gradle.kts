plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.theolm.bydapps.gecko.core"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    api(libs.geckoview)
    api(libs.androidx.media)
    api(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
