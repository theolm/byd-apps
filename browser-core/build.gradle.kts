plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.theolm.bydapps.core"
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
    api(libs.androidx.activity)
    // Required by the Kotlin 2.3 compiler workaround below, not by app code.
    api(libs.androidx.compose.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
}
