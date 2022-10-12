package org.babyfish.jimmer.sql.kt.loader.impl

import org.babyfish.jimmer.sql.loader.ReferenceLoader
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.loader.KReferenceLoader
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.impl.LambdaFieldFilterWrapper
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

    override fun forFilter(filter: KFilterDsl<T>.() -> Unit): KReferenceLoader<S, T> =
        javaLoader.forFilter(LambdaFieldFilterWrapper(filter)).let {
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