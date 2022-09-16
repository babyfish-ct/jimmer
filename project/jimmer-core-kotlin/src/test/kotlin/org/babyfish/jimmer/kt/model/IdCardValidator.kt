package org.babyfish.jimmer.kt.model

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class IdCardValidator : ConstraintValidator<IdCard, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean =
        value?.let {
            it.length == 15 || it.length == 18
        } ?: true
}