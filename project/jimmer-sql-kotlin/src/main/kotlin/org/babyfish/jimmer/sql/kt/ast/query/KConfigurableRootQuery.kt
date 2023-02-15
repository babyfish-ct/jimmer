package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.lang.NewChain
import java.sql.Connection

interface KConfigurableRootQuery<E: Any, R> : KTypedRootQuery<R> {

    fun count(con: Connection? = null): Int =
        reselect { select(org.babyfish.jimmer.sql.kt.ast.expression.count(table)) }
            .withoutSortingAndPaging()
            .execute(con)[0]
            .toInt()

    @NewChain
    fun <X> reselect(
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, X>
    ): KConfigurableRootQuery<E, X>

    @NewChain
    fun distinct(): KConfigurableRootQuery<E, R>

    @NewChain
    fun limit(limit: Int, offset: Int): KConfigurableRootQuery<E, R>

    @NewChain
    fun withoutSortingAndPaging(): KConfigurableRootQuery<E, R>

    @NewChain
    fun forUpdate(forUpdate: Boolean = true): KConfigurableRootQuery<E, R>
}