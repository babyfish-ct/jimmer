package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.Page
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.ast.query.PageFactory
import org.babyfish.jimmer.sql.kt.ast.expression.rowCount
import org.babyfish.jimmer.sql.kt.ast.query.impl.KConfigurableRootQueryImplementor
import java.sql.Connection

interface KConfigurableRootQuery<E: Any, R> : KTypedRootQuery<R> {

    fun fetchCount(con: Connection? = null): Long =
        reselect { select(rowCount()) }
            .withoutSortingAndPaging()
            .execute(con)[0]

    fun <P: Any> fetchPage(
        pageIndex: Int,
        pageSize: Int,
        con: Connection? = null,
        pageFactory: PageFactory<R, P>
    ): P

    fun fetchPage(
        pageIndex: Int,
        pageSize: Int,
        con: Connection? = null
    ): Page<R> = fetchPage(pageIndex, pageSize, con, PageFactory.standard())

    @NewChain
    fun <X> reselect(
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, X>
    ): KConfigurableRootQuery<E, X>

    @NewChain
    fun distinct(): KConfigurableRootQuery<E, R>

    @NewChain
    fun limit(limit: Int): KConfigurableRootQuery<E, R>

    @NewChain
    fun offset(offset: Long): KConfigurableRootQuery<E, R>

    @NewChain
    fun limit(limit: Int, offset: Long): KConfigurableRootQuery<E, R>

    @NewChain
    fun withoutSortingAndPaging(): KConfigurableRootQuery<E, R>

    /**
     * @return If the original query does not have `order by` clause, returns null
     */
    @NewChain
    fun reverseSorting(): KConfigurableRootQuery<E, R>?

    @NewChain
    fun forUpdate(forUpdate: Boolean = true): KConfigurableRootQuery<E, R>
}
