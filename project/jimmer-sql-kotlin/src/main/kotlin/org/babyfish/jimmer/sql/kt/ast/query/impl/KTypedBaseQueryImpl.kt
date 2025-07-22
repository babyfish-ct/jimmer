package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.ast.query.KTypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable

internal class KTypedBaseQueryImpl<T: KBaseTable>(
    private val _javaQuery: TypedBaseQuery<BaseTable>
) : KTypedBaseQuery<T> {

    override fun asBaseTable(): T {
        TODO("Not yet implemented")
    }

    override fun asCteBaseTable(): T {
        TODO("Not yet implemented")
    }
}