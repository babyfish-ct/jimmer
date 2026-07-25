package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableBaseTable

interface KMutableBaseTableQuery<B : KNonNullBaseTable<*>> : KMutableQuery<B> {

    override val table: B

    val selections: KMutableBaseQuery.Selections

    fun <
            T : KNonNullBaseTable<NT>,
            NT : KNullableBaseTable
            > select(projection: KBaseTableProjection<T, NT>): KConfigurableBaseQuery<T>
}
