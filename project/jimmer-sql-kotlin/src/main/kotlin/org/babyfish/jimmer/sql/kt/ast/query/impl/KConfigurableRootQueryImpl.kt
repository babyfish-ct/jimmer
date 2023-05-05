package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQueryImplementor
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import java.util.function.BiFunction

internal class KConfigurableRootQueryImpl<E: Any, R>(
    javaQuery: ConfigurableRootQuery<Table<E>, R>
) : KTypedRootQueryImpl<R>(javaQuery), KConfigurableRootQueryImplementor<E, R> {

    @Suppress("UNCHECKED_CAST")
    override val javaQuery: ConfigurableRootQuery<Table<E>, R>
        get() = super.javaQuery as ConfigurableRootQuery<Table<E>, R>

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

    override fun limit(limit: Int, offset: Int): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.limit(limit, offset))

    override fun withoutSortingAndPaging(): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.withoutSortingAndPaging())

    override fun forUpdate(forUpdate: Boolean): KConfigurableRootQuery<E, R> =
        KConfigurableRootQueryImpl(javaQuery.forUpdate(forUpdate))

    override val javaOrders: List<Order>
        get() = (javaQuery as ConfigurableRootQueryImplementor<*, *>).orders

    override val javaSqlClient: JSqlClient
        get() = (javaQuery as ConfigurableRootQueryImplementor<*, *>).sqlClient
}