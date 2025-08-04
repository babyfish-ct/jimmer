package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.PageSource
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery
import org.babyfish.jimmer.sql.ast.query.PageFactory
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import java.sql.Connection
import java.util.function.BiFunction

internal class KConfigurableRootQueryImpl<P: KPropsLike, R>(
    javaQuery: ConfigurableRootQuery<TableLike<*>, R>
) : KTypedRootQueryImpl<R>(javaQuery),
    KConfigurableRootQuery<P, R> {

    @Suppress("UNCHECKED_CAST")
    override val javaQuery: ConfigurableRootQuery<TableLike<*>, R>
        get() = super.javaQuery as ConfigurableRootQuery<TableLike<*>, R>

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
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).mutableQuery
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
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).mutableQuery
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
                    (javaQuery as ConfigurableRootQueryImpl<*, *>).mutableQuery
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
                (javaQuery as ConfigurableRootQueryImpl<*, *>).mutableQuery
            )
        )
    }

    override fun fetchSlice(limit: Int, offset: Int, con: Connection?): Slice<R> =
        javaQuery.fetchSlice(limit, offset, con)

    override fun <X> reselect(
        block: KMutableRootQuery<P>.() -> KConfigurableRootQuery<P, X>
    ): KConfigurableRootQuery<P, X> {
        val javaBlock = BiFunction<MutableRootQuery<TableLike<*>>, TableLike<*>, ConfigurableRootQuery<TableLike<*>, X>> { query, _ ->
            // TODO: Not only for entity
            val q = KMutableRootQueryImpl.ForEntityImpl<Any>(query as MutableRootQueryImpl<TableLike<*>>) as KMutableRootQuery<P>
            (q.block() as KConfigurableRootQueryImpl<P, X>).javaQuery
        }
        return KConfigurableRootQueryImpl(javaQuery.reselect(javaBlock))
    }

    override fun distinct(): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.distinct())

    override fun limit(limit: Int): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.limit(limit))

    override fun offset(offset: Long): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.offset(offset))

    override fun limit(limit: Int, offset: Long): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.limit(limit, offset))

    override fun withoutSortingAndPaging(): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.withoutSortingAndPaging())

    override fun reverseSorting(): KConfigurableRootQuery<P, R>? =
        javaQuery.reverseSorting()?.let {
            KConfigurableRootQueryImpl(it)
        }

    override fun setReverseSortOptimizationEnabled(
        enabled: Boolean
    ): KConfigurableRootQuery<P, R>? =
        KConfigurableRootQueryImpl(javaQuery.setReverseSortOptimizationEnabled(enabled))

    override fun forUpdate(forUpdate: Boolean): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.forUpdate(forUpdate))

    override fun hint(hint: String?): KConfigurableRootQuery<P, R> =
        KConfigurableRootQueryImpl(javaQuery.hint(hint))
}