package org.babyfish.jimmer.sql.kt.ast.table

class KBaseTableSymbol<T : KNonNullBaseTable<*>>(
    internal val baseTable: T
) {
    override fun toString(): String {
        return "KBastTableSymbol(${baseTable})"
    }
}