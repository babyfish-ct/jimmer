package org.babyfish.jimmer.sql.kt.loader

import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import java.sql.Connection

interface KReferenceLoader<S: Any, T: Any> {

    @NewChain
    fun forConnection(con: Connection): KReferenceLoader<S, T>

    @NewChain
    fun forFilter(filter: KFieldFilterDsl<T>.() -> Unit): KReferenceLoader<S, T>

    fun load(source: S, con: Connection? = null): T?

    fun batchLoad(sources: Collection<S>, con: Connection? = null): Map<S, T>
}