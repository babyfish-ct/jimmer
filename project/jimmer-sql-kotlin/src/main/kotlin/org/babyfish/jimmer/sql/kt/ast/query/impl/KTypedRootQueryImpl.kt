package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.query.TypedRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KTypedRootQuery
import java.sql.Connection

internal open class KTypedRootQueryImpl<R>(
    private val _javaQuery: TypedRootQuery<R>
) : KTypedRootQuery<R> {

    protected open val javaQuery: TypedRootQuery<R>
        get() = _javaQuery

    override fun union(other: KTypedRootQuery<R>): KTypedRootQuery<R> =
        KTypedRootQueryImpl(_javaQuery.union((other as KTypedRootQueryImpl<R>)._javaQuery))

    override fun unionAll(other: KTypedRootQuery<R>): KTypedRootQuery<R> =
        KTypedRootQueryImpl(_javaQuery.unionAll((other as KTypedRootQueryImpl<R>)._javaQuery))

    override fun minus(other: KTypedRootQuery<R>): KTypedRootQuery<R> =
        KTypedRootQueryImpl(_javaQuery.minus((other as KTypedRootQueryImpl<R>)._javaQuery))

    override fun intersect(other: KTypedRootQuery<R>): KTypedRootQuery<R> =
        KTypedRootQueryImpl(_javaQuery.intersect((other as KTypedRootQueryImpl<R>)._javaQuery))

    override fun execute(con: Connection?): List<R> =
        _javaQuery.execute(con)

    override fun forEach(con: Connection?, batchSize: Int, block: (R) -> Unit) {
        _javaQuery.forEach(con, batchSize, block)
    }
}