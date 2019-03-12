package com.dmdirc

import io.mockk.mockk
import io.mockk.verify
import javafx.beans.property.SimpleBooleanProperty
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class JoinDialogControllerTest {

    private val mockMainController = mockk<MainContract.Controller>()
    private val controller = JoinDialogController(mockMainController)

    @Test
    fun `calls joinChannel on main controller when joinChannel called`() {
        controller.join("#test123")
        verify {
            mockMainController.joinChannel("#test123")
        }
    }
}

internal class JoinDialogModelTest {

    private val mockController = mockk<JoinDialogContract.Controller>()
    private val truthValidator = SimpleBooleanProperty(true)
    private val falseValidator = SimpleBooleanProperty(false)
    private val model = JoinDialogModel(mockController)

    @Test
    fun `defaults to dialog being open`() {
        assertTrue(model.open.value)
    }

    @Test
    fun `closes when cancel pressed`() {
        model.onCancelPressed()
        assertFalse(model.open.value)
    }

    @Test
    fun `joins when valid and join pressed`() {
        model.valid.addValidator(truthValidator)
        model.channel.set("#dmdirc")
        model.onJoinPressed()
        verify {
            mockController.join("#dmdirc")
        }
    }

    @Test
    fun `closes when valid and join pressed`() {
        model.valid.addValidator(truthValidator)
        model.channel.set("#dmdirc")
        model.onJoinPressed()
        assertFalse(model.open.value)
    }

    @Test
    fun `joins when valid and text submitted`() {
        model.valid.addValidator(truthValidator)
        model.channel.set("#dmdirc")
        model.onTextAction()
        verify {
            mockController.join("#dmdirc")
        }
    }

    @Test
    fun `closes when valid and text submitted`() {
        model.valid.addValidator(truthValidator)
        model.channel.set("#dmdirc")
        model.onTextAction()
        assertFalse(model.open.value)
    }

    @Test
    fun `does nothing when invalid and join pressed`() {
        model.valid.addValidator(falseValidator)
        model.onJoinPressed()
        assertTrue(model.open.value)
        verify(inverse = true) {
            mockController.join(any())
        }
    }

    @Test
    fun `does nothing when invalid and text submitted`() {
        model.valid.addValidator(falseValidator)
        model.onTextAction()
        assertTrue(model.open.value)
        verify(inverse = true) {
            mockController.join(any())
        }
    }
}
