package org.babyfish.jimmer.spring.model

class Page<E>(
    val totalRowCount: Int,
    val totalPageCount: Int,
    val entities: List<E>
)