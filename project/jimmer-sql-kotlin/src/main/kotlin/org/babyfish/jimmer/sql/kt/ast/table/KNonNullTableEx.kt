package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.JoinType
import kotlin.reflect.KClass

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E> {

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        joinType: JoinType = JoinType.INNER,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullTable<E>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, TT>>
    ): TT =
        weakJoin(targetSymbol, JoinType.INNER, weakJoinType)

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        joinType: JoinType,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, TT>>
    ): TT

    override fun <X : Any> weakJoin(
        targetType: KClass<X>,
        weakJoinFun: KWeakJoinFun<E, X>
    ): KNonNullTableEx<X>

    override fun <X : Any> weakJoin(
        weakJoinType: KClass<out KWeakJoin<E, X>>
    ): KNonNullTableEx<X>
}
