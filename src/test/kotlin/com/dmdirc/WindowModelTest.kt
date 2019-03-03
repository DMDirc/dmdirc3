package com.dmdirc

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class WindowModelTest {

    private val mockConnection = mockk<ConnectionContract.Controller>()

    @Test
    fun `uses connectionId in sort key if provided`() {
        val model = WindowModel("name", WindowType.ROOT, null, false, "cid123")
        assertEquals("cid123 name", model.sortKey)
    }

    @Test
    fun `doesn't use connectionId in sort key if null`() {
        val model = WindowModel("name", WindowType.ROOT, null, false, null)
        assertEquals(" name", model.sortKey)
    }

    @Test
    fun `doesn't use name in sort key if server`() {
        val model = WindowModel("name", WindowType.ROOT, null, true, "cid123")
        assertEquals("cid123 ", model.sortKey)
    }

    @Test
    fun `uses blank sort key if server without connection id`() {
        val model = WindowModel("name", WindowType.ROOT, null, true, null)
        assertEquals(" ", model.sortKey)
    }

    @Test
    fun `does nothing when handling input if field is blank`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, true, null)
        model.handleInput()
        verify(inverse = true) {
            mockConnection.sendMessage(any(), any())
        }
    }

    @Test
    fun `sends message to controller when input is not empty`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, true, null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        verify {
            mockConnection.sendMessage("name", "Mess with the best")
        }
    }

    @Test
    fun `blanks input field after sending message`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, true, null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        assertEquals("", model.inputField.value)
    }

}