package org.babyfish.jimmer.kt.model

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class IdCardValidator : ConstraintValidator<IdCard, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return true
    }
}