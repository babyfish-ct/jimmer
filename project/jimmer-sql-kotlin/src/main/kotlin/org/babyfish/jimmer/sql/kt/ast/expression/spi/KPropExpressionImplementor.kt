package org.babyfish.jimmer.sql.kt.ast.expression.spi

import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import kotlin.reflect.KProperty1

interface KPropExpressionImplementor<T: Any> : KPropExpression<T> {

    fun <X: Any> get(prop: KProperty1<T, X?>): KPropExpression<X>
}