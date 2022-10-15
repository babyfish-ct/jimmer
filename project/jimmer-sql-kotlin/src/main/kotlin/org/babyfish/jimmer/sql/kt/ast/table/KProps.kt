package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import kotlin.reflect.KProperty1

interface KProps<E: Any> {

    fun <X: Any, EXP: KPropExpression<X>> get(prop: String): EXP

    fun <X: Any> join(prop: String): KTable<X>
    fun <X: Any> joinReference(prop: KProperty1<E, X?>): KTable<X>
    fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KTable<X>

    fun <X: Any> outerJoin(prop: String): KNullableTable<X>
    fun <X: Any> outerJoinReference(prop: KProperty1<E, X?>): KNullableTable<X>
    fun <X: Any> outerJoinList(prop: KProperty1<E, List<X>>): KNullableTable<X>

    fun <X: Any> inverseJoin(backProp: ImmutableProp): KTable<X>
    fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KTable<X>
    fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KTable<X>

    fun <X: Any> inverseOuterJoin(backProp: ImmutableProp): KNullableTable<X>
    fun <X: Any> inverseOuterJoinReference(backProp: KProperty1<X, E?>): KNullableTable<X>
    fun <X: Any> inverseOuterJoinList(backProp: KProperty1<X, List<E>>): KNullableTable<X>
}