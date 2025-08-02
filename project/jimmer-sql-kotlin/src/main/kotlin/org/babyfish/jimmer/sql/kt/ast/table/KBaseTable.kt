package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Selection
import kotlin.reflect.KClass

interface KBaseTable : KPropsLike

interface KNonNullBaseTable<NT: KNullableBaseTable> : KBaseTable

interface KNullableBaseTable : KBaseTable

interface KNonNullBaseTable1<
    T1: Selection<*>,
    T1Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable1<T1Nullable>> {

    val _1: T1

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable1<T1: Selection<*>> : KNullableBaseTable {

    val _1: T1

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1>, TT>>
    ): TNT
}

interface KNonNullBaseTable2<
    T1: Selection<*>,
    T2: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable2<T1Nullable, T2Nullable>> {

    val _1: T1

    val _2: T2

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable2<
    T1: Selection<*>,
    T2: Selection<*>,
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1, T2>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1, T2>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1, T2>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1, T2>, TT>>
    ): TNT
}

interface KNonNullBaseTable3<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable3<T1Nullable, T2Nullable, T3Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable3<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>>
    ): TNT
}

interface KNonNullBaseTable4<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable4<T1Nullable, T2Nullable, T3Nullable, T4Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable4<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>
>: KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>>
    ): TNT
}

interface KNonNullBaseTable5<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>,
    T5Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable5<T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable5<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>>
    ): TNT
}

interface KNonNullBaseTable6<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>,
    T5Nullable: Selection<*>,
    T6Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable6<T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable6<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>>
    ): TNT
}

interface KNonNullBaseTable7<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>,
    T5Nullable: Selection<*>,
    T6Nullable: Selection<*>,
    T7Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable7<T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable7<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>>
    ): TNT
}

interface KNonNullBaseTable8<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>,
    T5Nullable: Selection<*>,
    T6Nullable: Selection<*>,
    T7Nullable: Selection<*>,
    T8Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable8<T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    val _8: T8

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable8<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    val _8: T8

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>>
    ): TNT
}

interface KNonNullBaseTable9<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
    T9: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>,
    T4Nullable: Selection<*>,
    T5Nullable: Selection<*>,
    T6Nullable: Selection<*>,
    T7Nullable: Selection<*>,
    T8Nullable: Selection<*>,
    T9Nullable: Selection<*>
> : KNonNullBaseTable<KNullableBaseTable9<T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>> {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    val _8: T8

    val _9: T9

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>>
    ): TNT
}

interface KNullableBaseTable9<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
    T9: Selection<*>
> : KNullableBaseTable {

    val _1: T1

    val _2: T2

    val _3: T3

    val _4: T4

    val _5: T5

    val _6: T6

    val _7: T7

    val _8: T8

    val _9: T9

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>
    ): TT

    fun <TT: KNonNullBaseTable<*>> weakJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>>
    ): TT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>
    ): TNT

    fun <TNT: KNullableBaseTable, TT: KNonNullBaseTable<TNT>> weakOuterJoin(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>>
    ): TNT
}
