package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.EmbeddableDto
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KNonNullEmbeddedPropExpression<T: Any> : KNonNullPropExpression<T>, KEmbeddedPropExpression<T> {

    fun fetch(fetcher: Fetcher<T>?): Selection<T>

    fun <V: EmbeddableDto<T>> fetch(valueType: KClass<V>): Selection<V>
}