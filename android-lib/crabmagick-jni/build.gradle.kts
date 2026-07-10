// build.gradle.kts for the Android library that packages the JNI .so files
plugins {
    id("com.android.library") version "8.5.0"
    kotlin("android") version "2.3.21"
    `maven-publish`
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        // AndroidX namespace for the library
        namespace = "io.github.anaruto.libcrabmagick"
        ndk {
            // Build for the most common ABIs; others can be added later
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    buildTypes {
        release {
            // The .so files are pre‑built by Cargo; just package them
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Package the .so files that Cargo will produce under jniLibs
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    // No other dependencies needed for the thin wrapper
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("java.library.path", file("src/main/jniLibs/arm64-v8a").absolutePath)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "io.github.anaruto"
            artifactId = "libcrabmagick"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
