package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.TypedTuple
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookViewForTupleTest

@TypedTuple
data class DtoTuple(
    val book: BookViewForTupleTest,
    val authorCount: Long
)