package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQuerySource
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.PageFactory
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import java.sql.Connection
import java.util.function.BiFunction

internal class KConfigurableRootQueryImpl<E: Any, R>(
    javaQuery: ConfigurableRootQuery<Table<E>, R>
) : KTypedRootQueryImpl<R>(javaQuery),
    KConfigurableRootQuery<E, R>,
    ConfigurableRootQuerySource {

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
                this
            )
        }
        if (pageIndex < 0) {
            return pageFactory.create(
                emptyList(),
                0,
                this
            )
        }

        val offset = pageIndex.toLong() * pageSize
        require(offset <= Long.MAX_VALUE - pageSize) { "offset is too big" }
        val total = this.fetchCount(con)
        if (offset >= total) {
            return pageFactory.create(
                emptyList(),
                total,
                this
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
                    .reversed()
            } else {
                this
                    .limit(pageSize, offset)
                    .execute(con)
            }
        return pageFactory.create(
            entities,
            total,
            this
        )
    }

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

    override fun forUpdate(forUpdate: Boolean): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.forUpdate(forUpdate))

    override fun getOrders(): List<Order> =
        (javaQuery as ConfigurableRootQuerySource).orders

    override fun getLimit(): Int =
        (javaQuery as ConfigurableRootQuerySource).limit

    override fun getSqlClient(): JSqlClientImplementor =
        (javaQuery as ConfigurableRootQuerySource).sqlClient
}