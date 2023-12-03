package org.babyfish.jimmer.sql.kt.api


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ThrowsSystemError(vararg val value: SystemErrorCode)
