package io.github.anaruto.libcrabmagick

object CrabMagick {
    init {
        System.loadLibrary("crabmagick")
    }

    @JvmStatic
    external fun nativeApplyMagic(input: ByteArray): ByteArray
}
