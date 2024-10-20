package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.exception.EmptyResultException
import org.babyfish.jimmer.sql.exception.IncorrectResultSizeException
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.runtime.ExecutionException
import java.sql.Connection

interface KTypedRootQuery<R> : KExecutable<List<R>> {

    infix fun union(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix fun unionAll(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix operator fun minus(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix fun intersect(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    fun fetchOne(con: Connection? = null): R =
        execute(con).let {
            when (it.size) {
                0 -> throw EmptyResultException(1)
                1 -> it[0]
                else -> throw IncorrectResultSizeException(1, it.size)
            }
        }

    fun fetchOneOrNull(con: Connection? = null): R? =
        execute(con).let {
            when (it.size) {
                0 -> null
                1 -> it[0]
                else -> throw IncorrectResultSizeException(1, it.size)
            }
        }

    fun <X> map(con: Connection? = null, mapper: (R) -> X): List<X>

    fun forEach(
        con: Connection? = null,
        batchSize: Int = 0,
        block: (R) -> Unit
    )
}