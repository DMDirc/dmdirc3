package com.dmdirc

import io.mockk.mockk
import io.mockk.verify
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ValidatorChainTest {

    private val chain = ValidatorChain()

    @Test
    fun `is invalid by default`() {
        assertFalse(chain.value)
    }

    @Test
    fun `becomes valid when a passing validator is added`() {
        chain.addValidator(SimpleBooleanProperty(true))
        assertTrue(chain.value)
    }

    @Test
    fun `remains invalid when a failing validator is added`() {
        chain.addValidator(SimpleBooleanProperty(false))
        assertFalse(chain.value)
    }

    @Test
    fun `listens for changes to added validators`() {
        val prop = SimpleBooleanProperty(false)
        chain.addValidator(prop)
        assertFalse(chain.value)
        prop.set(true)
        assertTrue(chain.value)
    }

    @Test
    fun `notifies listeners when a new validator affects its state`() {
        val listener = mockk<ChangeListener<Boolean>>()
        chain.addListener(listener)
        chain.addValidator(SimpleBooleanProperty(true))
        verify { listener.changed(chain, false, true) }
    }

    @Test
    fun `does not notify listeners when a new validator doesn't affect its state`() {
        chain.addValidator(SimpleBooleanProperty(true))

        val listener = mockk<ChangeListener<Boolean>>()
        chain.addListener(listener)
        chain.addValidator(SimpleBooleanProperty(true))
        verify(inverse = true) { listener.changed(refEq(chain), any(), any()) }
    }

    @Test
    fun `notifies listeners when a validator causes the state to change`() {
        val prop = SimpleBooleanProperty(true)
        chain.addValidator(prop)

        val listener = mockk<ChangeListener<Boolean>>()
        chain.addListener(listener)

        prop.set(false)

        verify { listener.changed(chain, true, false) }
    }

    @Test
    fun `does not notify listeners when a validator is updated but doesn't affect its state`() {
        val prop = SimpleBooleanProperty(true)
        chain.addValidator(prop)
        chain.addValidator(SimpleBooleanProperty(false))

        val listener = mockk<ChangeListener<Boolean>>()
        chain.addListener(listener)

        prop.set(false)

        verify(inverse = true) { listener.changed(refEq(chain), any(), any()) }
    }
}