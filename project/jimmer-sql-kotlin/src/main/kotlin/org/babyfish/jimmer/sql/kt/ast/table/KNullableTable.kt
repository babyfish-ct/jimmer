package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KNullableTable<E: Any> : KTable<E>, Selection<E?> {
    override fun <X: Any> join(prop: String): KNullableTable<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNullableTable<X>
    fun fetch(fetcher: Fetcher<E>): Selection<E?>
}