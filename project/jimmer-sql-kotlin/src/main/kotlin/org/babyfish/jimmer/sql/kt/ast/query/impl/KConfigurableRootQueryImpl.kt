package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.PageSource
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery
import org.babyfish.jimmer.sql.ast.query.PageFactory
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import java.sql.Connection
import java.util.Collections
import java.util.function.BiFunction

internal class KConfigurableRootQueryImpl<E: Any, R>(
    javaQuery: ConfigurableRootQuery<Table<E>, R>
) : KTypedRootQueryImpl<R>(javaQuery),
    KConfigurableRootQuery<E, R> {

    @Suppress("UNCHECKED_CAST")
    override val javaQuery: ConfigurableRootQuery<Table<E>, R>
        get() = super.javaQuery as ConfigurableRootQuery<Table<E>, R>

    override fun <P : Any> fetchPage(
        pageIndex: Int,
        pageSize: Int,
        con: Connection?,
        pageFactory: PageFactory<R, P>
    ): P {
        if (pageSize == 0 || pageSize == -1 || pageSize == Int.MAX_VALUE) {
            val rows = this.execute(con)
            return pageFactory.create(
                rows,
                rows.size.toLong(),
                PageSource.of(
                    0,
                    Int.MAX_VALUE,
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).baseQuery
                )
            )
        }
        if (pageIndex < 0) {
            return pageFactory.create(
                emptyList(),
                0,
                PageSource.of(
                    0,
                    pageSize,
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).baseQuery
                )
            )
        }

        val offset = pageIndex.toLong() * pageSize
        require(offset <= Long.MAX_VALUE - pageSize) { "offset is too big" }
        val total = this.fetchUnlimitedCount(con)
        if (offset >= total) {
            return pageFactory.create(
                emptyList(),
                total,
                PageSource.of(
                    pageIndex,
                    pageSize,
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).baseQuery
                )
            )
        }

        val reversedQuery = this
            .takeIf { offset + pageSize / 2 > total / 2 }
            ?.reverseSorting()

        val entities: List<R> =
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
                    .let {
                        // Why not `.reversed()`, see #889
                        if (it.size < 2) {
                            it
                        } else {
                            val newList = it.toMutableList()
                            newList.reverse()
                            newList
                        }
                    }
            } else {
                this
                    .limit(pageSize, offset)
                    .execute(con)
            }
        return pageFactory.create(
            entities,
            total,
            PageSource.of(
                pageIndex,
                pageSize,
                (javaQuery as ConfigurableRootQueryImpl<*, *>).baseQuery
            )
        )
    }

    override fun fetchSlice(limit: Int, offset: Int, con: Connection?): Slice<R> =
        javaQuery.fetchSlice(limit, offset, con)

    override fun <X> reselect(
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, X>
    ): KConfigurableRootQuery<E, X> {
        val javaBlock = BiFunction<MutableRootQuery<Table<E>>, Table<E>, ConfigurableRootQuery<Table<E>, X>> { query, _ ->
            (KMutableRootQueryImpl(query as MutableRootQueryImpl<Table<E>>).block() as KConfigurableRootQueryImpl<E, X>).javaQuery
        }
        return KConfigurableRootQueryImpl(javaQuery.reselect(javaBlock))
    }

    override fun distinct(): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.distinct())

    override fun limit(limit: Int): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.limit(limit))

    override fun offset(offset: Long): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.offset(offset))

    override fun limit(limit: Int, offset: Long): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.limit(limit, offset))

    override fun withoutSortingAndPaging(): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.withoutSortingAndPaging())

    override fun reverseSorting(): KConfigurableRootQuery<E, R>? =
        javaQuery.reverseSorting()?.let {
            KConfigurableRootQueryImpl(it)
        }

    override fun setReverseSortOptimizationEnabled(enabled: Boolean): KConfigurableRootQuery<E, R>? =
        KConfigurableRootQueryImpl(javaQuery.setReverseSortOptimizationEnabled(enabled))

    override fun forUpdate(forUpdate: Boolean): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.forUpdate(forUpdate))

    override fun hint(hint: String?): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.hint(hint))
}