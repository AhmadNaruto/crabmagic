# CrabMagick Android Library

Pure-Rust image processing library for Android. Zero C++ dependencies, no CMake wrappers, and no external native packages needed. Optimized for the **arm64-v8a** Android platform using **ARM Neon** hardware acceleration.

---

## Supported Codecs

### Input Formats (Decoders)
* **JPEG XL (`.jxl`)** — via native Rust modular JXL decoder.
* **JPEG (`.jpg`, `.jpeg`)**
* **PNG (`.png`)**
* **WebP (`.webp`)**
* **TIFF (`.tiff`)**
* **BMP (`.bmp`)**
* **GIF (`.gif`)**
* **PNM (`.pnm`)**

### Output Formats (Encoders)
* **JPEG XL (`.jxl`)** — via native Rust encoder.
* **JPEG (`.jpg`)** — supports progressive JPEGs and two-pass Huffman table optimization.
* **WebP (`.webp`)**
* **PNG (`.png`)**
* **TIFF (`.tiff`)**

---

## Getting Started

### 1. Adding via JitPack (Recommended)

To use this library in your Android project, add the JitPack repository and the library dependency:

**In `settings.gradle` or root `build.gradle`:**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

**In your app's `build.gradle`:**
```groovy
dependencies {
    implementation 'com.github.anaruto:crabmagick:1.0.0'
}
```

---

## Kotlin API Usage

Invoke the native processing core directly from your Kotlin/Java code:

```kotlin
import io.github.anaruto.libcrabmagick.CrabMagick

// Input: raw image bytes (e.g. read from an asset or file)
val inputBytes: ByteArray = ...

// Output: processed JPEG XL (or other format) bytes
try {
    val outputBytes: ByteArray = CrabMagick.nativeApplyMagic(inputBytes)
    // Save outputBytes or load into Bitmap
} catch (e: RuntimeException) {
    Log.e("CrabMagick", "Processing failed: ${e.message}")
}
```

---

## Building Locally

To build the project and compile the native Rust binaries locally (e.g., inside Termux or on your development machine):

### Requirements
* **Android SDK** and **NDK (Side-by-side) 26.1+**
* **Rust toolchain** with Android targets:
  ```bash
  rustup target add aarch64-linux-android
  cargo install cargo-ndk
  ```

### Build Commands
To run the automated build and package the library into an AAR binary:

```bash
# Compile Rust core and assemble the Android AAR
./gradlew -p android-lib clean :crabmagick-native:cargoBuild :crabmagick-jni:assembleRelease
```

The output `.aar` package will be generated at:
`android-lib/crabmagick-jni/build/outputs/aar/crabmagick-jni-release.aar`

---

## Architecture Design

CrabMagick Android is a pure Rust-to-Java JNI bridge:
1. **`crabmagick-core`**: A Rust crate that wraps the image processing engine and exports JNI symbols (`Java_io_github_anaruto_libcrabmagick_CrabMagick_nativeApplyMagic`).
2. **`crabmagick-jni`**: A lightweight Android library module wrapping the compiled `libcrabmagick.so` dynamic library and presenting a clean Kotlin class interface.
3. **`cargoBuild` task**: Automates the compilation of Rust code targeting `aarch64-linux-android` with Neon vector optimization (`-C target-feature=+neon`), and automatically copies the biner to the correct `jniLibs` path prior to library assembly.
