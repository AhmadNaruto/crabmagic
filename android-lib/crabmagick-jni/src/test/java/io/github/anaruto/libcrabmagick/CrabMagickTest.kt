package io.github.anaruto.libcrabmagick

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * JVM unit tests for the CrabMagick Kotlin wrapper.
 *
 * These tests run on the host JVM (x86_64 Linux on CI), so the Android native
 * library (aarch64-linux-android) cannot be loaded. All tests are therefore
 * designed to:
 *   1. Verify the Kotlin class structure (methods, signatures) via reflection, OR
 *   2. Tolerate UnsatisfiedLinkError / ExceptionInInitializerError gracefully,
 *      because those errors confirm the correct code path exists — just not the native binary.
 *
 * Actual end-to-end native tests run on a real Android device via instrumented tests.
 */
class CrabMagickTest {

    /**
     * Verifies that CrabMagick is an object (singleton) and that nativeApplyMagic
     * exists with the correct signature — without triggering the static initializer.
     */
    @Test
    fun testNativeApplyMagicMethodSignatureExists() {
        val clazz = Class.forName("io.github.anaruto.libcrabmagick.CrabMagick")
        val method = clazz.getDeclaredMethod("nativeApplyMagic", ByteArray::class.java)
        assertNotNull("nativeApplyMagic method must exist", method)
        assertEquals(
            "Return type must be ByteArray",
            ByteArray::class.java,
            method.returnType
        )
        assertTrue(
            "nativeApplyMagic must be static (@JvmStatic on object)",
            Modifier.isStatic(method.modifiers)
        )
    }

    /**
     * Verifies that invoking nativeApplyMagic either:
     *   - Succeeds and returns a non-null ByteArray (when the native lib IS present), or
     *   - Throws UnsatisfiedLinkError / ExceptionInInitializerError (on host JVM without the .so)
     *
     * Any other exception type is a real failure.
     */
    @Test
    fun testNativeApplyMagicBehavesOrThrowsLinkError() {
        try {
            val output = CrabMagick.nativeApplyMagic(ByteArray(0))
            assertNotNull("Native method returned null", output)
        } catch (e: UnsatisfiedLinkError) {
            // Expected on host JVM — no aarch64-android .so available.
        } catch (e: ExceptionInInitializerError) {
            // Triggered when System.loadLibrary fails during class init.
            val cause = e.cause
            assertTrue(
                "Expected UnsatisfiedLinkError as root cause, got: ${cause?.javaClass}",
                cause is UnsatisfiedLinkError
            )
        } catch (e: RuntimeException) {
            // Only acceptable if the message matches our defined error message.
            assertTrue(
                "Unexpected RuntimeException: ${e.message}",
                e.message?.contains("crabmagick") == true
            )
        }
    }
}
