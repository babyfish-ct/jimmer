package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.lang.NewChain

interface KConfigurableTypedRootQuery<E: Any, R> : KTypedRootQuery<R> {

    @NewChain
    fun <X> reselect(
        block: KMutableRootQuery<E>.() -> KConfigurableTypedRootQuery<E, X>
    ): KConfigurableTypedRootQuery<E, X>

    @NewChain
    fun distinct(): KConfigurableTypedRootQuery<E, R>?

    @NewChain
    fun limit(limit: Int, offset: Int): KConfigurableTypedRootQuery<E, R>

    @NewChain
    fun withoutSortingAndPaging(): KConfigurableTypedRootQuery<E, R>

    @NewChain
    fun forUpdate(): KConfigurableTypedRootQuery<E, R>
}