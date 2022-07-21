package org.babyfish.jimmer.sql.kt.annotations

@Target(AnnotationTarget.PROPERTY)
annotation class KJoinColumn(
    val name: String
)
