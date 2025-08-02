package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.RecursiveBaseQueryCreator
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.RecursiveRef
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMergedBaseQueryImpl
import org.babyfish.jimmer.sql.kt.ast.table.*

interface KTypedBaseQuery<T: KNonNullBaseTable<*>> {

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
        recursiveBaseQueryCreator: (KRecursiveRef<T>) -> KConfigurableBaseQuery<T>
    ): KTypedBaseQuery<T> {
        val javaCreator = RecursiveBaseQueryCreator {
            val kotlinRef = KRecursiveRef<T>(it)
            val kotlinBaseQuery = recursiveBaseQueryCreator(kotlinRef)
            KMergedBaseQueryImpl.javaBaseQuery(kotlinBaseQuery)
        }
        val mergedJavaQuery = TypedBaseQuery.unionAllRecursively(
            KMergedBaseQueryImpl.javaBaseQuery(this),
            javaCreator
        ) as MergedBaseQueryImpl
        return KMergedBaseQueryImpl(this, mergedJavaQuery)
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

inline fun <T: KNonNullBaseTable<*>> baseTableSymbol(
    queryCreator: () -> KTypedBaseQuery<T>
): KBaseTableSymbol<T> =
    queryCreator().asBaseTable()

inline fun <T: KNonNullBaseTable<*>> cteBaseTableSymbol(
    queryCreator: () -> KTypedBaseQuery<T>
): KBaseTableSymbol<T> =
    queryCreator().asCteBaseTable()