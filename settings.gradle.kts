pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MoneyTick"
include(":core")
// The Android app needs the Android SDK. Skip it with -PcoreOnly=true on machines without one.
if (providers.gradleProperty("coreOnly").orNull != "true") {
    include(":app")
}
