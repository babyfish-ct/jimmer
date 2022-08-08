package org.babyfish.jimmer.sql.kt.loader

import org.babyfish.jimmer.lang.NewChain
import java.sql.Connection

interface KValueLoader<S: Any, V> {

    @NewChain
    fun forConnection(con: Connection): KValueLoader<S, V>

    fun load(source: S): V

    fun batchLoad(sources: Collection<S>): Map<S, V>
}