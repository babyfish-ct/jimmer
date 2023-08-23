package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.lang.NewChain
import java.sql.Connection

interface KConfigurableRootQuery<E: Any, R> : KTypedRootQuery<R> {

    fun count(con: Connection? = null): Long =
        reselect { select(org.babyfish.jimmer.sql.kt.ast.expression.count(table)) }
            .withoutSortingAndPaging()
            .execute(con)[0]

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
