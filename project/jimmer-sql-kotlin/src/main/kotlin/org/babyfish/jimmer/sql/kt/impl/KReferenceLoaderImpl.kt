package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.ReferenceLoader
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.KReferenceLoader
import org.babyfish.jimmer.sql.kt.fetcher.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.fetcher.impl.KFilter
import java.sql.Connection

internal class KReferenceLoaderImpl<S: Any, T: Any>(
    private val javaLoader: ReferenceLoader<S, T, Table<T>>
): KReferenceLoader<S, T> {

    override fun forConnection(con: Connection): KReferenceLoader<S, T> =
        javaLoader.forConnection(con).let {
            if (javaLoader === it) {
                this
            } else {
                KReferenceLoaderImpl(it)
            }
        }

    override fun forFilter(filter: KFilter<T>): KReferenceLoader<S, T> =
        javaLoader.forFilter(FilterWrapper(filter)).let {
            if (javaLoader === it) {
                this
            } else {
                KReferenceLoaderImpl(it)
            }
        }

    override fun load(source: S, con: Connection?): T? =
        javaLoader.loadCommand(source).execute(con)

    override fun batchLoad(sources: Collection<S>, con: Connection?): Map<S, T> =
        javaLoader.batchLoadCommand(sources).execute(con)
}