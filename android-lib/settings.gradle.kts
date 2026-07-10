// settings.gradle.kts for the multi‑module Android JNI refactor
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "crabmagick-android"
include(":crabmagick-jni")
include(":crabmagick-native")
