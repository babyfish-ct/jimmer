package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.EmbeddableDto
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNullableEmbeddedPropExpression<T: Any> : KEmbeddedPropExpression<T>, KNullablePropExpression<T> {

    override fun <X: Any> get(prop: KProperty1<T, X?>): KNullablePropExpression<X>

    override fun <X: Any> get(prop: ImmutableProp): KNullablePropExpression<X>

    fun fetch(fetcher: Fetcher<T>?): Selection<T?>

    fun <V: EmbeddableDto<T>> fetch(valueType: KClass<V>): Selection<V?>
}