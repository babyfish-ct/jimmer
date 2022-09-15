package org.babyfish.jimmer.kt.model

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class RegisterRequestValidator : ConstraintValidator<SamePassword, RegisterRequest> {

    override fun isValid(value: RegisterRequest?, context: ConstraintValidatorContext?): Boolean {
        TODO("Not yet implemented")
    }
}