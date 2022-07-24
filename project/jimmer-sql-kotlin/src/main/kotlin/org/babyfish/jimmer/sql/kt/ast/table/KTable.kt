package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import kotlin.reflect.KClass

interface KTable<E: Any> {
    fun <X: Any, EXP: KPropExpression<X>> get(prop: String): EXP
    fun <X: Any> join(prop: String): KTable<X>
    fun <X: Any> outerJoin(prop: String): KNullableTable<X>
    fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KTable<X>
    fun <X: Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTable<X>
}