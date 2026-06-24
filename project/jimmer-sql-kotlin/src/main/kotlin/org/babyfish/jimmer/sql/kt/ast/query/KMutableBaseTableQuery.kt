package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable

interface KMutableBaseTableQuery<B : KNonNullBaseTable<*>> : KMutableQuery<B> {

    override val table: B

    val selections: KMutableBaseQuery.Selections
}
