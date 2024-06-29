package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KNonNullEmbeddedPropExpression<T: Any> : KNonNullPropExpression<T>, KEmbeddedPropExpression<T> {

    fun fetch(fetcher: Fetcher<T>?): Selection<T>

    fun <V: View<T>> fetch(staticType: KClass<V>): Selection<V>
}