package org.babyfish.jimmer.sql.kt.loader

import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import java.sql.Connection

interface KListLoader<S: Any, T: Any> {

    @NewChain
    fun forConnection(con: Connection): KListLoader<S, T>

    @NewChain
    fun forFilter(filter: KFieldFilterDsl<T>.() -> Unit): KListLoader<S, T>

    fun load(source: S, con: Connection? = null): List<T>

    fun batchLoad(sources: Collection<S>, con: Connection? = null): Map<S, List<T>>
}