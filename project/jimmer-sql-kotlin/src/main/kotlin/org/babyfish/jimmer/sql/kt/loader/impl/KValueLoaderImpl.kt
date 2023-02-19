package org.babyfish.jimmer.sql.kt.loader.impl

import org.babyfish.jimmer.sql.kt.loader.KValueLoader
import org.babyfish.jimmer.sql.loader.graphql.ValueLoader
import java.sql.Connection

class KValueLoaderImpl<S: Any, V>(
    private val javaLoader: ValueLoader<S, V>
) : KValueLoader<S, V> {

    override fun forConnection(con: Connection): KValueLoader<S, V> =
        javaLoader.forConnection(con).let {
            if (javaLoader === it) {
                this
            } else {
                KValueLoaderImpl(it)
            }
        }

    override fun load(source: S): V =
        javaLoader.load(source)

    override fun batchLoad(sources: Collection<S>): Map<S, V> =
        javaLoader.batchLoad(sources)
}