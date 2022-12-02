package org.babyfish.jimmer.sql.kt.ast.expression.spi

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import kotlin.reflect.KProperty1

interface KNonNullPropExpressionImplementor<T: Any> : KNonNullPropExpression<T> {

    fun <X: Any> get(prop: KProperty1<T, X>): KNonNullPropExpression<X>
}