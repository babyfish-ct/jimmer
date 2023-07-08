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
        totalCount: Int,
        queryImplementor: KConfigurableRootQueryImplementor<*, E>
    ) -> P
): P {
    val queryImplementor = this as KConfigurableRootQueryImplementor<*, E>
    if (pageSize == 0) {
        val entities = this.execute(con)
        return pageFactory(
            entities,
            entities.size,
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

    val longOffset = pageIndex.toLong() * pageSize
    require(longOffset <= Int.MAX_VALUE - pageSize) { "offset is too big" }
    val total = this.count(con)
    if (longOffset >= total) {
        return pageFactory(
            emptyList(),
            0,
            queryImplementor
        )
    }

    val reversedQuery = this
        .takeIf { longOffset + pageSize / 2 > total / 2 }
        ?.reverseSorting()

    val entities: List<E> =
        if (reversedQuery != null) {
            var offset = (total - longOffset - pageSize).toInt()
            val limit = if (offset < 0) {
                (pageSize + offset).also {
                    offset = 0
                }
            } else {
                pageSize
            }
            reversedQuery
                .limit(limit, offset)
                .execute(con)
                .reversed()
        } else {
            this
                .limit(pageSize, longOffset.toInt())
                .execute(con)
        }
    return pageFactory(
        entities,
        total,
        queryImplementor
    )
}