package org.babyfish.jimmer.sql.kt.meta

data class Generated<T: Any>(
    val javaClass: Class<T>
)