package com.dmdirc

import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.events.ChannelNickChanged
import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.events.ChannelQuit
import com.dmdirc.ktirc.events.ChannelUserKicked
import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.model.User
import com.dmdirc.ui.nicklist.NickListModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NickListModelTest {

    private val model = NickListModel()
    private val metaData = mockk<EventMetadata>()

    @Test
    fun `starts with an empty list`() {
        assertTrue(model.users.isEmpty())
    }

    @Test
    fun `adds user on join`() {
        model.handleEvent(ChannelJoined(metaData, User("acidBurn"), "#channel"))
        assertEquals(1, model.users.size)
        assertEquals("acidBurn", model.users[0])
    }

    @Test
    fun `renames user on nick change`() {
        model.users += "acidBurn"
        model.users += "zeroCool"
        model.handleEvent(ChannelNickChanged(metaData, User("zeroCool"), "#channel", "crashOverride"))
        assertEquals(2, model.users.size)
        assertEquals("acidBurn", model.users[0])
        assertEquals("crashOverride", model.users[1])
    }

    @Test
    fun `removes user on part`() {
        model.users += "acidBurn"
        model.users += "zeroCool"
        model.handleEvent(ChannelParted(metaData, User("zeroCool"), "#channel"))
        assertEquals(1, model.users.size)
        assertEquals("acidBurn", model.users[0])
    }

    @Test
    fun `removes user on quit`() {
        model.users += "acidBurn"
        model.users += "zeroCool"
        model.handleEvent(ChannelQuit(metaData, User("zeroCool"), "#channel"))
        assertEquals(1, model.users.size)
        assertEquals("acidBurn", model.users[0])
    }

    @Test
    fun `removes user on kick`() {
        model.users += "acidBurn"
        model.users += "zeroCool"
        model.handleEvent(ChannelUserKicked(metaData, User("acidBurn"), "#channel", "zeroCool"))
        assertEquals(1, model.users.size)
        assertEquals("acidBurn", model.users[0])
    }

    @Test
    fun `clears existing list when users replaced`() {
        model.users += "acidBurn"
        model.users += "zeroCool"
        model.handleEvent(mockk {
            every { addedUser } returns null
            every { removedUser } returns null
            every { replacedUsers } returns emptyArray()
        })
        assertEquals(0, model.users.size)
    }

    @Test
    fun `adds new users when replaced`() {
        model.handleEvent(mockk {
            every { addedUser } returns null
            every { removedUser } returns null
            every { replacedUsers } returns arrayOf("acidBurn", "zeroCool")
        })
        assertEquals(2, model.users.size)
        assertEquals("acidBurn", model.users[0])
        assertEquals("zeroCool", model.users[1])
    }
}
