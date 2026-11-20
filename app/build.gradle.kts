import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.dudu26.moneytick"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.dudu26.moneytick"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.0"
    }

    // Release signing comes from environment variables (set by CI from repository secrets).
    // Without them the build falls back to the debug key, so local builds still work.
    val keystorePath = System.getenv("MT_KEYSTORE_FILE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                // Trim and fall back so a blank or space-padded secret does not break signing.
                val storePw = System.getenv("MT_KEYSTORE_PASSWORD")?.trim()
                storePassword = storePw
                keyAlias = System.getenv("MT_KEY_ALIAS")?.trim()?.takeIf { it.isNotEmpty() } ?: "moneytick"
                keyPassword = System.getenv("MT_KEY_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() } ?: storePw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // ScreenshotTest renders the screens with Robolectric and writes docs/screenshots/*.png.
            all { it.systemProperty("roborazzi.test.record", "true") }
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
