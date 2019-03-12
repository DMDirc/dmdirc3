package com.dmdirc

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyBooleanPropertyBase
import javafx.beans.value.ChangeListener
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import kotlinx.atomicfu.atomic
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import org.controlsfx.validation.decoration.StyleClassValidationDecoration

val requiredValidator: Validator<TextField> = Validator.createEmptyValidator<TextField>("Required")

fun bindRequiredTextControl(c: TextInputControl, p: Property<String>, m: ValidatingModel) {
    bindTextControl(c, p, requiredValidator, m)
}

fun bindTextControl(c: TextInputControl, p: Property<String>, v: Validator<TextField>, m: ValidatingModel) {
    val validationSupport = ValidationSupport()
    validationSupport.validationDecorator = StyleClassValidationDecoration("validation-error", "validation-warning")
    validationSupport.registerValidator(c, v)
    m.valid.addValidator(validationSupport.invalidProperty().not())
    c.textProperty().set(p.value)
    p.bindBidirectional(c.textProperty())
}

interface ValidatingModel {
    val valid: ValidatorChain
}

/**
 * Manages a chain of validators that must all pass in order for the overall validation state to be true.
 *
 * If no validators are supplied, validation fails. Listeners are only notified when the overall validation
 * state changes.
 */
class ValidatorChain : ReadOnlyBooleanPropertyBase() {

    private val validators = mutableListOf<BooleanExpression>()
    private var valid = atomic(false)

    private val listener = ChangeListener<Boolean> { _, _, _ -> recompute() }

    override fun getName() = ""
    override fun getBean() = null

    override fun get() = validators.isNotEmpty() && validators.all { it.get() }

    fun addValidator(validator: BooleanExpression) {
        validators.add(validator)
        validator.addListener(listener)
        recompute()
    }

    private fun recompute() {
        val newValue = get()
        if (valid.compareAndSet(!newValue, newValue)) {
            fireValueChangedEvent()
        }
    }
}
