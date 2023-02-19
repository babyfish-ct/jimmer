package org.babyfish.jimmer.sql.example.bll.error

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BusinessThrows(
    val value: Array<BusinessErrorCode>
)