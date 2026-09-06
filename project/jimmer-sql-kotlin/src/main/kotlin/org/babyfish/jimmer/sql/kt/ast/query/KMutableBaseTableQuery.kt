package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.*

interface KMutableBaseTableQuery<B : KPropsLike> : KMutableQuery<B> {

    override val table: B

    val selections: KMutableBaseQuery.Selections

    fun <T : Any> select(table: KNonNullTable<T>): KConfigurableBaseQuery<KNonNullTable<T>>

    fun <T : Any> select(table: KNullableTable<T>): KConfigurableBaseQuery<KNullableTable<T>>

    fun <
            T : KNonNullBaseTable<NT>,
            NT : KNullableBaseTable
            > select(projection: KBaseTableProjection<T, NT>): KConfigurableBaseQuery<T>
}
