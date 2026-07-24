package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableShape
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableBaseTable

interface KBaseTableProjection<
        T : KNonNullBaseTable<NT>,
        NT : KNullableBaseTable
        > {

    fun getSelections(): List<Selection<*>>

    fun getBaseTableShape(): BaseTableShape<T, NT>

    fun getSelectionTypes(): ByteArray
}
