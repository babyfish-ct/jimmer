package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.ListLoader
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.KListLoader
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.impl.FilterWrapper
import java.sql.Connection

internal class KListLoaderImpl<S: Any, T: Any>(
    private val javaLoader: ListLoader<S, T, Table<T>>
): KListLoader<S, T> {

    override fun forConnection(con: Connection): KListLoader<S, T> =
        javaLoader.forConnection(con).let {
            if (javaLoader === it) {
                this
            } else {
                KListLoaderImpl(it)
            }
        }

    override fun forFilter(filter: KFilterDsl<T>.() -> Unit): KListLoader<S, T> =
        javaLoader.forFilter(FilterWrapper(filter)).let {
            if (javaLoader === it) {
                this
            } else {
                KListLoaderImpl(it)
            }
        }

    override fun load(source: S, con: Connection?): List<T> =
        javaLoader.loadCommand(source).execute(con)

    override fun batchLoad(sources: Collection<S>, con: Connection?): Map<S, List<T>> =
        javaLoader.batchLoadCommand(sources).execute(con)
}