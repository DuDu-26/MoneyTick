// The Android Gradle plugin is declared in :app only, so :core can build on machines
// without access to Google's Maven repository (use -PcoreOnly=true there).
plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
