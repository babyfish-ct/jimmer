package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.TypedTuple
import org.babyfish.jimmer.sql.kt.model.classic.book.Book

@TypedTuple
data class EntityTuple(
    val book: Book,
    val authorCount: Long
)