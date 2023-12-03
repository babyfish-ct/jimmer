package org.babyfish.jimmer.client.meta

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Api(
    vararg val value: String = []
)