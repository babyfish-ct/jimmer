package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Selection

interface KNullableTable<E: Any> : KTable<E>, Selection<E?> {
    override fun <X: Any> join(prop: String): KNullableTable<X>
    override fun <X: Any> inverseJoin(prop: String): KNullableTable<X>
}