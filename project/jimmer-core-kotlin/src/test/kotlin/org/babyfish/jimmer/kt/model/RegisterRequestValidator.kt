package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.kt.isLoaded
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class RegisterRequestValidator : ConstraintValidator<SamePassword, RegisterRequest> {

    override fun isValid(value: RegisterRequest?, context: ConstraintValidatorContext?): Boolean =
        value?.let {
            if (isLoaded(it, RegisterRequest::password) && isLoaded(it, RegisterRequest::passwordAgain)) {
                it.password == it.passwordAgain
            } else {
                null
            }
        } ?: true
}