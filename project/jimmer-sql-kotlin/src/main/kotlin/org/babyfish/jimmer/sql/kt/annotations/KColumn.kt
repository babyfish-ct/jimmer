package org.babyfish.jimmer.sql.kt.annotations

@Target(AnnotationTarget.PROPERTY)
annotation class KColumn(
    val name: String
)