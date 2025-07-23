package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KProps<E: Any> : KPropsLike {

    fun <X: Any> get(prop: String): KPropExpression<X>
    fun <X: Any> get(prop: ImmutableProp): KPropExpression<X>
    fun <X: Any> getId(): KPropExpression<X>
    fun <X: Any> getAssociatedId(prop: String): KPropExpression<X>
    fun <X: Any> getAssociatedId(prop: ImmutableProp): KPropExpression<X>

    fun <X: Any> join(prop: String): KNonNullTable<X>
    fun <X: Any> join(prop: ImmutableProp): KNonNullTable<X>
    fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNonNullTable<X>
    fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTable<X>

    fun <X: Any> outerJoin(prop: String): KNullableTable<X>
    fun <X: Any> outerJoin(prop: ImmutableProp): KNullableTable<X>
    fun <X: Any> outerJoinReference(prop: KProperty1<E, X?>): KNullableTable<X>
    fun <X: Any> outerJoinList(prop: KProperty1<E, List<X>>): KNullableTable<X>

    fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTable<X>
    fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTable<X>
    fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTable<X>

    fun <X: Any> inverseOuterJoin(backProp: ImmutableProp): KNullableTable<X>
    fun <X: Any> inverseOuterJoinReference(backProp: KProperty1<X, E?>): KNullableTable<X>
    fun <X: Any> inverseOuterJoinList(backProp: KProperty1<X, List<E>>): KNullableTable<X>

    fun <X: Any> exists(prop: String, block: KImplicitSubQueryTable<X>.() -> KNonNullExpression<Boolean>?): KNonNullExpression<Boolean>?
    fun <X: Any> exists(prop: ImmutableProp, block: KImplicitSubQueryTable<X>.() -> KNonNullExpression<Boolean>?): KNonNullExpression<Boolean>?
}
