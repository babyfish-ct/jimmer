package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.isNotNull
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import kotlin.reflect.KClass

interface KTable<E: Any> {
    fun <X: Any, EXP: KPropExpression<X>> get(prop: String): EXP
    fun <X: Any> join(prop: String): KTable<X>
    fun <X: Any> outerJoin(prop: String): KNullableTable<X>
    fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KTable<X>
    fun <X: Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTable<X>
}

fun KTable<*>.isNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection<*>).immutableType.idProp
    return get<Any, KPropExpression<Any>>(idProp.name).isNull()
}

fun KTable<*>.isNotNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection<*>).immutableType.idProp
    return get<Any, KPropExpression<Any>>(idProp.name).isNotNull()
}