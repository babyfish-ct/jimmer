package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KTableEx<E: Any> : KTable<E> {

    override fun <X: Any> join(prop: String): KNonNullTableEx<X>
    override fun <X: Any> join(prop: ImmutableProp): KNonNullTableEx<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNonNullTableEx<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTableEx<X>

    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> outerJoin(prop: ImmutableProp): KNullableTableEx<X>
    override fun <X: Any> outerJoinReference(prop: KProperty1<E, X?>): KNullableTableEx<X>
    override fun <X: Any> outerJoinList(prop: KProperty1<E, List<X>>): KNullableTableEx<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTableEx<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTableEx<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTableEx<X>

    override fun <X: Any> inverseOuterJoin(backProp: ImmutableProp): KNullableTableEx<X>
    override fun <X: Any> inverseOuterJoinReference(backProp: KProperty1<X, E?>): KNullableTableEx<X>
    override fun <X: Any> inverseOuterJoinList(backProp: KProperty1<X, List<E>>): KNullableTableEx<X>

    fun <X: Any> weakJoin(targetType: KClass<X>, weakJoinFun: KWeakJoinFun<E, X>): KNonNullTableEx<X>
    fun <X: Any> weakJoin(weakJoinType: KClass<out KWeakJoin<E, X>>): KNonNullTableEx<X>
    fun <X: Any> weakOuterJoin(targetType: KClass<X>, weakJoinFun: KWeakJoinFun<E, X>): KNullableTableEx<X>
    fun <X: Any> weakOuterJoin(weakJoinType: KClass<out KWeakJoin<E, X>>): KNullableTableEx<X>

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullTable<E>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullTable<E>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<*>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, TT>>
    ): TNT
}