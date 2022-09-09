package org.babyfish.jimmer.example.kt.sql.model

data class Page<E>(
    val entities: List<E>,
    val totalRowCount: Int,
    val totalPageCount: Int
)