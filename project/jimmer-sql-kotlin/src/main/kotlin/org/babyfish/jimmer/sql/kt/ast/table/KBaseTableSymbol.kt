package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.impl.base.BaseTableProxies
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.AbstractKBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

class KBaseTableSymbol<T : KPropsLike>(
    internal val baseTable: T
) {
    internal val javaTable: BaseTable
        get() = when (val table = baseTable) {
            is AbstractKBaseTable -> table.javaTable
            is KTableImplementor<*> -> BaseTableProxies.resolve(table.javaTable)
            else -> error("Unsupported base-table symbol: $table")
        }

    override fun toString(): String {
        return "KBastTableSymbol(${baseTable})"
    }
}
