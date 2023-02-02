package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.Dto
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KNullableTable<E: Any> : KTable<E>, KNullableProps<E>, Selection<E?> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E?>

    fun <S: Dto<E>> fetch(staticType: KClass<S>): Selection<E?>

    override fun asTableEx(): KNullableTableEx<E>
}