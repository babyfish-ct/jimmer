package org.babyfish.jimmer.sql.kt.loader.impl

import org.babyfish.jimmer.sql.loader.ListLoader
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.impl.LambdaFieldFilterWrapper
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
        javaLoader.forFilter(LambdaFieldFilterWrapper(filter)).let {
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