package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.ast.query.KTypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable

internal class KTypedBaseQueryImpl<T: KNonNullBaseTable<*>>(
    private val _javaQuery: TypedBaseQuery<BaseTable>
) : KTypedBaseQuery<T> {

    override fun asBaseTable(): KBaseTableSymbol<T> {
        TODO("Not yet implemented")
    }

    override fun asCteBaseTable(): KBaseTableSymbol<T> {
        TODO("Not yet implemented")
    }
}