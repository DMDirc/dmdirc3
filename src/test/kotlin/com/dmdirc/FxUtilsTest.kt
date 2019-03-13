package com.dmdirc

import com.dmdirc.PlatformWrappers.fxThreadTester
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class FxUtilsTest {

    private val exception = slot<Throwable>()
    private val mockReporter = mockk<ErrorReporter> {
        every { notify(capture(exception)) } just Runs
    }

    private fun uiOperation() {
        assertOnFxThread(mockReporter)
    }

    private fun doUiCall() {
        uiOperation()
    }

    @Test
    fun `assertOnFxThread puts method name in message`() {
        fxThreadTester = { false }
        doUiCall()
        assertNotNull(exception.captured)
        assertEquals(true, exception.captured.message?.startsWith("Function uiOperation must be called on the FX thread"))
    }

    @Test
    fun `assertOnFxThread rewrites stack trace to point at the bad function`() {
        fxThreadTester = { false }
        doUiCall()
        assertNotNull(exception.captured)
        val firstMethod = exception.captured.stackTrace?.get(0)?.methodName
        assertEquals("doUiCall", firstMethod)
    }

    @Test
    fun `assertOnFxThread does nothing if on the fx thread`() {
        fxThreadTester = { true }
        doUiCall()
        verify(inverse = true) {
            mockReporter.notify(any())
        }
    }
}
