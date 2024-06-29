package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNullableEmbeddedPropExpression<T: Any> : KNullablePropExpression<T> {

    fun <X: Any> get(prop: KProperty1<T, X?>): KNullablePropExpression<X>

    fun <X: Any> get(prop: ImmutableProp): KNullablePropExpression<X>

    fun fetch(fetcher: Fetcher<T>?): Selection<T?>

    fun <V: View<T>> fetch(staticType: KClass<V>): Selection<V?>
}