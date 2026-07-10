package io.github.anaruto.libcrabmagick

import org.junit.Assert.*
import org.junit.Test

class CrabMagickTest {

    @Test
    fun testLibraryLoadsWithoutException() {
        // Trigger class initialization
        Class.forName("io.github.anaruto.libcrabmagick.CrabMagick")
    }

    @Test
    fun testNativeApplyMagicBehavesAsExpected() {
        val dummyInput = ByteArray(0)
        try {
            val output = CrabMagick.nativeApplyMagic(dummyInput)
            assertNotNull("Native method returned null", output)
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("crabmagick processing failed") == true)
        }
    }
}
