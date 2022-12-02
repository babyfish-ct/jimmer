package org.babyfish.jimmer.sql.kt.ast.expression.spi

import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import kotlin.reflect.KProperty1

interface KNullablePropExpressionImplementor<T: Any> : KNullablePropExpression<T> {

    fun <X: Any> get(prop: KProperty1<T, X>): KNullablePropExpression<X>
}