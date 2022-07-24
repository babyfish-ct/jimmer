package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.lang.NewChain

interface KConfigurableRootQuery<E: Any, R> : KTypedRootQuery<R> {

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
    fun forUpdate(): KConfigurableRootQuery<E, R>
}