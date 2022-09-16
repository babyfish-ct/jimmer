package org.babyfish.jimmer.kt.model

import javax.validation.Constraint

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [RegisterRequestValidator::class])
annotation class SamePassword(
    val message: String = "The passwords must be same"
)