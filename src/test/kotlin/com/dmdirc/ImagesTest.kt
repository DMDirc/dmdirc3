package com.dmdirc

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ImagesTest {

    private val url = "https://localhost/image.png"

    private val mockConfig = mockk<ClientConfig>()

    private val fakeModel = WindowModel("", WindowType.CHANNEL, null, mockConfig, null)

    @BeforeEach
    fun setup() {
        fakeModel.addImageHandler(mockConfig)
        runLaterProvider = { it.run() }
    }

    @Test
    fun `does nothing when a normal line is added with display images enabled`() {
        every { mockConfig[ClientSpec.Display.embedImages] } returns true
        fakeModel.lines.add(arrayOf(StyledSpan("Mess with the best", emptySet())))
        assertEquals(1, fakeModel.lines.size)
    }

    @Test
    fun `does nothing when an image link is added with the config setting disabled`() {
        every { mockConfig[ClientSpec.Display.embedImages] } returns false
        fakeModel.lines.add(arrayOf(StyledSpan(url, setOf(Style.Link(url)))))
        assertEquals(1, fakeModel.lines.size)
    }

    @Test
    fun `adds an image when a single image link is added`() {
        every { mockConfig[ClientSpec.Display.embedImages] } returns true
        fakeModel.lines.add(arrayOf(StyledSpan(url, setOf(Style.Link(url)))))
        assertEquals(2, fakeModel.lines.size)
        assertArrayEquals(arrayOf(StyledSpan("${ControlCode.InternalImages}$url", emptySet())), fakeModel.lines[1])
    }

    @Test
    fun `adds a single image line with multiple images when multiple links are added in one line`() {
        every { mockConfig[ClientSpec.Display.embedImages] } returns true
        fakeModel.lines.add(
            arrayOf(
                StyledSpan(url, setOf(Style.Link(url))),
                StyledSpan(url, setOf(Style.Link(url))),
                StyledSpan(url, setOf(Style.Link(url)))
            )
        )
        assertEquals(2, fakeModel.lines.size)
        assertArrayEquals(
            arrayOf(
                StyledSpan("${ControlCode.InternalImages}$url", emptySet()),
                StyledSpan("${ControlCode.InternalImages}$url", emptySet()),
                StyledSpan("${ControlCode.InternalImages}$url", emptySet())
            ), fakeModel.lines[1]
        )
    }

    @Test
    fun `adds a multiple image lines when multiple lines containing image links are added in one go`() {
        every { mockConfig[ClientSpec.Display.embedImages] } returns true
        fakeModel.lines.addAll(
            arrayOf(StyledSpan(url, setOf(Style.Link(url)))), arrayOf(StyledSpan(url, setOf(Style.Link(url))))
        )
        assertEquals(4, fakeModel.lines.size)
        assertArrayEquals(
            arrayOf(StyledSpan("${ControlCode.InternalImages}$url", emptySet())), fakeModel.lines[1]
        )
        assertArrayEquals(
            arrayOf(StyledSpan("${ControlCode.InternalImages}$url", emptySet())), fakeModel.lines[3]
        )
    }
}
