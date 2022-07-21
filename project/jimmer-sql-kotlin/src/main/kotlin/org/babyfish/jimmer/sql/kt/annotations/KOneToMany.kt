package org.babyfish.jimmer.sql.kt.annotations

@Target(AnnotationTarget.PROPERTY)
annotation class KOneToMany(
    val mappedBy: String
)