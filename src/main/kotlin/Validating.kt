package com.dmdirc

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.StringProperty
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator

val requiredValidator: Validator<TextField> = Validator.createEmptyValidator<TextField>("Required")

fun bindRequiredTextControl(c: TextInputControl, p: StringProperty, m: ValidatingModel) {
    val validationSupport = ValidationSupport()
    validationSupport.registerValidator(c, requiredValidator)
    validationSupport.invalidProperty()
    m.addValidator(validationSupport.invalidProperty())
    p.bindBidirectional(c.textProperty())
}

interface ValidatingModel {
    fun addValidator(validator: BooleanExpression)
}