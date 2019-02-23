
import com.dmdirc.ClientConfig
import com.dmdirc.ClientSpec
import com.dmdirc.SettingsModel
import com.dmdirc.kodein
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider

private class SettingsModelTest {

    private val mockConfig = mockk<ClientConfig>()

    @BeforeEach
    fun setup() {
        val original = kodein
        kodein = Kodein {
            extend(original, allowOverride = true)
            bind<ClientConfig>(overrides = true) with provider { mockConfig }
        }
    }

    @Test
    fun `defaults profile fields to values from config`() {
        every { mockConfig[ClientSpec.DefaultProfile.nickname] } returns "acidBurn"
        every { mockConfig[ClientSpec.DefaultProfile.username] } returns "burn"
        every { mockConfig[ClientSpec.DefaultProfile.realname] } returns "Libby"

        val model = SettingsModel()
        assertEquals("acidBurn", model.nickname.value)
        assertEquals("burn", model.username.value)
        assertEquals("Libby", model.realname.value)
    }

    @Test
    fun `updates config with new profile values and saves on commit`() {
        every { mockConfig[ClientSpec.DefaultProfile.nickname] } returns "zeroCool"
        every { mockConfig[ClientSpec.DefaultProfile.username] } returns "zero"
        every { mockConfig[ClientSpec.DefaultProfile.realname] } returns "Dade"

        val model = SettingsModel()
        model.nickname.value = "crashOverride"
        model.username.value = "crash"
        model.realname.value = "???"
        model.onCommit()

        verify {
            mockConfig[ClientSpec.DefaultProfile.nickname] = "crashOverride"
            mockConfig[ClientSpec.DefaultProfile.username] = "crash"
            mockConfig[ClientSpec.DefaultProfile.realname] = "???"
            mockConfig.save()
        }
    }

}
