package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher

interface KNullableTable<E: Any> : KTable<E>, KNullableProps<E>, Selection<E?> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E?>

    override fun asTableEx(): KNullableTableEx<E>
}