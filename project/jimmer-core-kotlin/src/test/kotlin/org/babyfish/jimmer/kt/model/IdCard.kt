package org.babyfish.jimmer.kt.model

import javax.validation.Constraint

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
@Constraint(validatedBy = [IdCardValidator::class])
annotation class IdCard(
    val message: String
)