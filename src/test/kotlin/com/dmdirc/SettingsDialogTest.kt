package com.dmdirc
import com.dmdirc.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javafx.beans.property.SimpleBooleanProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class SettingsModelTest {

    private val mockController = mockk<SettingsDialogContract.Controller>()
    private val mockConfig = mockk<ClientConfig>()

    @Test
    fun `defaults profile fields to values from config`() {
        every { mockConfig[ClientSpec.DefaultProfile.nickname] } returns "acidBurn"
        every { mockConfig[ClientSpec.DefaultProfile.username] } returns "burn"
        every { mockConfig[ClientSpec.DefaultProfile.realname] } returns "Libby"

        val model = SettingsDialogModel(mockController, mockConfig)
        assertEquals("acidBurn", model.nickname.value)
        assertEquals("burn", model.username.value)
        assertEquals("Libby", model.realname.value)
    }

    @Test
    fun `updates config with new profile values and saves on commit`() {
        every { mockConfig[ClientSpec.DefaultProfile.nickname] } returns "acidBurn"
        every { mockConfig[ClientSpec.DefaultProfile.username] } returns "burn"
        every { mockConfig[ClientSpec.DefaultProfile.realname] } returns "Libby"

        val model = SettingsDialogModel(mockController, mockConfig)
        val truthValidator = SimpleBooleanProperty(true)
        model.valid.addValidator(truthValidator)
        model.nickname.value = "crashOverride"
        model.username.value = "crash"
        model.realname.value = "???"
        model.onSavePressed()

        verify {
            mockController.save("crashOverride", "???", "crash")
        }
    }

    @Test
    fun `test config saves on controller save`() {
        every { mockConfig[ClientSpec.DefaultProfile.nickname] } returns "acidBurn"
        every { mockConfig[ClientSpec.DefaultProfile.username] } returns "burn"
        every { mockConfig[ClientSpec.DefaultProfile.realname] } returns "Libby"

        val controller = SettingsDialogController(mockConfig)
        controller.save("crashOverride", "crash", "???")
        verify {
            mockConfig[ClientSpec.DefaultProfile.nickname] = "crashOverride"
            mockConfig[ClientSpec.DefaultProfile.realname] = "crash"
            mockConfig[ClientSpec.DefaultProfile.username] = "???"
            mockConfig.save()
        }
    }

}
