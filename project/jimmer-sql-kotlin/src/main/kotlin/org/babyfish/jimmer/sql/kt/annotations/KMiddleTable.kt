package org.babyfish.jimmer.sql.kt.annotations

@Target(AnnotationTarget.PROPERTY)
annotation class KMiddleTable(
    val name: String = "",
    val joinColumnName: String = "",
    val inverseJoinColumnName: String = ""
)
