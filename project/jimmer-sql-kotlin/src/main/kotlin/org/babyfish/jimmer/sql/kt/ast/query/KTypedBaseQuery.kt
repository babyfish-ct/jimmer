package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTableSymbol

interface KTypedBaseQuery<T: KBaseTable> {

    fun asBaseTable(): KBaseTableSymbol<T>

    fun asCteBaseTable(): KBaseTableSymbol<T>
}