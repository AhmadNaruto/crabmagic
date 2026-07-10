// build.gradle.kts for the "crabmagick-native" module that compiles the Rust core with cargo‑ndk
import java.io.File

plugins {
    // We only need the base plugin to define custom tasks
    `java`
}

val cargoBuild = tasks.register("cargoBuild") {
    val targetAbis = listOf("arm64-v8a")
    outputs.upToDateWhen { false }
    doLast {
        targetAbis.forEach { abi ->
            val ndkTarget = when (abi) {
                "arm64-v8a" -> "aarch64-linux-android"
                else -> abi
            }
            val pb = ProcessBuilder(
                "cargo", "ndk", "--target", ndkTarget, "build", "--release", "-p", "crabmagick-core"
            )
            pb.directory(project.file("../../rust"))
            val homeDir = System.getProperty("user.home")
            val cargoBin = "$homeDir/.cargo/bin"
            val currentPath = pb.environment()["PATH"] ?: ""
            pb.environment()["PATH"] = "$cargoBin:$currentPath"
            pb.environment()["RUSTFLAGS"] = "-C target-feature=+neon"
            pb.redirectErrorStream(true)
            val process = pb.start()
            val reader = process.inputStream.bufferedReader()
            var line = reader.readLine()
            while (line != null) {
                println(line)
                line = reader.readLine()
            }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw org.gradle.api.GradleException("Cargo build failed with exit code $exitCode")
            }

            // Copy the built library to the JNI module's jniLibs directory
            val rustTargetDir = project.file("../../rust/target/$ndkTarget/release")
            val jniLibsDir = project.file("../crabmagick-jni/src/main/jniLibs/$abi")
            jniLibsDir.mkdirs()
            val srcFile = File(rustTargetDir, "libcrabmagick.so")
            val destFile = File(jniLibsDir, "libcrabmagick.so")
            srcFile.copyTo(destFile, overwrite = true)
            println("Copied ${srcFile.absolutePath} to ${destFile.absolutePath}")
        }
    }
}


// Make the Android module depend on this task so the .so files are ready before packaging
tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn(cargoBuild)
    }
}
