package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMergedBaseQueryImpl
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.KRecursiveRef

interface KTypedBaseQuery<T: KBaseTable> {

    fun asBaseTable(): KBaseTableSymbol<T>

    fun asCteBaseTable(): KBaseTableSymbol<T>

    infix fun union(other: KTypedBaseQuery<T>): KTypedBaseQuery<T> =
        KMergedBaseQueryImpl(
            this,
            TypedBaseQuery.union(
                KMergedBaseQueryImpl.javaBaseQuery(this),
                KMergedBaseQueryImpl.javaBaseQuery(other)
            ) as MergedBaseQueryImpl<*>
        )

    infix fun unionAll(other: KTypedBaseQuery<T>): KTypedBaseQuery<T> =
        KMergedBaseQueryImpl(
            this,
            TypedBaseQuery.unionAll(
                KMergedBaseQueryImpl.javaBaseQuery(this),
                KMergedBaseQueryImpl.javaBaseQuery(other)
            ) as MergedBaseQueryImpl<*>
        )

    fun unionAllRecursively(
        recursiveRef: (KRecursiveRef<T>) -> KConfigurableBaseQuery<T>
    ): KTypedBaseQuery<T> {
        TODO()
    }

    infix fun minus(other: KTypedBaseQuery<T>): KTypedBaseQuery<T> =
        KMergedBaseQueryImpl(
            this,
            TypedBaseQuery.minus(
                KMergedBaseQueryImpl.javaBaseQuery(this),
                KMergedBaseQueryImpl.javaBaseQuery(other)
            ) as MergedBaseQueryImpl<*>
        )

    infix fun intersect(other: KTypedBaseQuery<T>): KTypedBaseQuery<T> =
        KMergedBaseQueryImpl(
            this,
            TypedBaseQuery.intersect(
                KMergedBaseQueryImpl.javaBaseQuery(this),
                KMergedBaseQueryImpl.javaBaseQuery(other)
            ) as MergedBaseQueryImpl<*>
        )
}

inline fun <T: KBaseTable> baseTableSymbol(
    queryCreator: () -> KTypedBaseQuery<T>
): KBaseTableSymbol<T> =
    queryCreator().asBaseTable()

inline fun <T: KBaseTable> cteBaseTableSymbol(
    queryCreator: () -> KTypedBaseQuery<T>
): KBaseTableSymbol<T> =
    queryCreator().asBaseTable()