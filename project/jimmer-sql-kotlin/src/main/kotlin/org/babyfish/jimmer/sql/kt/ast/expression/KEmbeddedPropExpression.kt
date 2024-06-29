package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KProperty1

interface KEmbeddedPropExpression<T: Any> : KPropExpression<T> {

    fun <X: Any> get(prop: KProperty1<T, X?>): KPropExpression<X>

    fun <X: Any> get(prop: ImmutableProp): KPropExpression<X>
}