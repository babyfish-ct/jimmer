package org.babyfish.jimmer.sql.kt.loader.impl

import org.babyfish.jimmer.sql.loader.graphql.FilterableListLoader
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.impl.JavaFieldFilter
import java.sql.Connection

internal class KListLoaderImpl<S: Any, T: Any>(
    private val javaLoader: FilterableListLoader<S, T, Table<T>>
): KListLoader<S, T> {

    override fun forConnection(con: Connection): KListLoader<S, T> =
        javaLoader.forConnection(con).let {
            if (javaLoader === it) {
                this
            } else {
                KListLoaderImpl(it)
            }
        }

    override fun forFilter(filter: KFieldFilterDsl<T>.() -> Unit): KListLoader<S, T> =
        javaLoader.forFilter(JavaFieldFilter(filter)).let {
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