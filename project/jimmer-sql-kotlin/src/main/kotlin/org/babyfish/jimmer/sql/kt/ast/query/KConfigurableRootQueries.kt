package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.query.impl.KConfigurableRootQueryImplementor
import java.sql.Connection

@Suppress("UNCHECKED_CAST")
fun <E, P> KConfigurableRootQuery<*, E>.fetchPage(
    pageIndex: Int,
    pageSize: Int,
    con: Connection? = null,
    pageFactory: (
        entities: List<E>,
        totalCount: Long,
        queryImplementor: KConfigurableRootQueryImplementor<*, E>
    ) -> P
): P {
    val queryImplementor = this as KConfigurableRootQueryImplementor<*, E>
    if (pageSize == 0 || pageSize == -1 || pageSize == Int.MAX_VALUE) {
        val entities = this.execute(con)
        return pageFactory(
            entities,
            entities.size.toLong(),
            queryImplementor
        )
    }
    if (pageIndex < 0) {
        return pageFactory(
            emptyList(),
            0,
            queryImplementor
        )
    }

    val offset = pageIndex.toLong() * pageSize
    require(offset <= Long.MAX_VALUE - pageSize) { "offset is too big" }
    val total = this.count(con)
    if (offset >= total) {
        return pageFactory(
            emptyList(),
            0,
            queryImplementor
        )
    }

    val reversedQuery = this
        .takeIf { offset + pageSize / 2 > total / 2 }
        ?.reverseSorting()

    val entities: List<E> =
        if (reversedQuery != null) {
            var reversedOffset = total - offset - pageSize
            val limit = if (reversedOffset < 0) {
                (pageSize + reversedOffset.toInt()).also {
                    reversedOffset = 0
                }
            } else {
                pageSize
            }
            reversedQuery
                .limit(limit, reversedOffset)
                .execute(con)
                .reversed()
        } else {
            this
                .limit(pageSize, offset)
                .execute(con)
        }
    return pageFactory(
        entities,
        total,
        queryImplementor
    )
}