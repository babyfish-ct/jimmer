package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.base.AbstractBaseTableSymbol
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableProxies
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.base.*
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableProxy
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableSelectionKind
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass

abstract class AbstractKBaseTable protected constructor(
    internal val javaTable: BaseTable
) : KBaseTable, BaseTableProxy {

    final override fun __unwrap(): BaseTable =
        javaTable

    override fun hashCode(): Int {
        return javaTable.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AbstractKBaseTable) {
            return false
        }
        return javaTable == other.javaTable
    }

    override fun toString(): String {
        return "K$javaTable"
    }

    protected fun <T : Selection<*>> selection(index: Int, nullable: Boolean): T {
        val symbol = javaTable as BaseTableSymbol
        return kotlinSelection(
            symbol.selections[index],
            symbol.selectionLayout[index],
            nullable
        )
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST : KBaseTable, TT : KNonNullBaseTable<*>> weakJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<ST, TT>
    ): TT {
        val handle = createPropsWeakJoinHandle(this::class.java, targetSymbol::class.java, weakJoinLambda)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTable).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.INNER,
            null
        )
        return nonNull(javaJoinedTable) as TT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST : KBaseTable, TT : KNonNullBaseTable<*>> weakJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<ST, TT>>
    ): TT {
        val handle = WeakJoinHandle.of(weakJoinType.java)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTable).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.INNER,
            null
        )
        return nonNull(javaJoinedTable) as TT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST : KBaseTable, TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<ST, TT>
    ): TNT {
        val handle = createPropsWeakJoinHandle(this::class.java, targetSymbol::class.java, weakJoinLambda)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTable).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.LEFT,
            null
        )
        return nullable(javaJoinedTable) as TNT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST : KBaseTable, TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<ST, TT>>
    ): TNT {
        val handle = WeakJoinHandle.of(weakJoinType.java)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTable).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.LEFT,
            null
        )
        return nullable(javaJoinedTable) as TNT
    }

    companion object {

        fun nonNull(baseTable: BaseTable): AbstractKBaseTable {
            val baseTable = BaseTableProxies.unwrap(baseTable)
            val symbol = baseTable as BaseTableSymbol
            val shaped = symbol.shape?.createNonNull(symbol)
            if (shaped is AbstractKBaseTable) {
                return shaped
            }
            return when (symbol.selections.size) {
                1 -> NonNullTable1<
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                2 -> NonNullTable2<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                3 -> NonNullTable3<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                4 -> NonNullTable4<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                5 -> NonNullTable5<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                6 -> NonNullTable6<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                7 -> NonNullTable7<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                8 -> NonNullTable8<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                9 -> NonNullTable9<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>
                        >(baseTable)

                else -> throw IllegalArgumentException()
            }
        }

        fun nullable(baseTable: BaseTable): AbstractKBaseTable {
            val baseTable = BaseTableProxies.unwrap(baseTable)
            val symbol = baseTable as BaseTableSymbol
            val shaped = symbol.shape?.createNullable(symbol)
            if (shaped is AbstractKBaseTable) {
                return shaped
            }
            return when (symbol.selections.size) {
                1 -> NullableTable1<
                        Selection<*>
                        >(baseTable)

                2 -> NullableTable2<
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                3 -> NullableTable3<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                4 -> NullableTable4<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                5 -> NullableTable5<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                6 -> NullableTable6<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                7 -> NullableTable7<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                8 -> NullableTable8<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                9 -> NullableTable9<
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        Selection<*>,
                        >(baseTable)

                else -> throw IllegalArgumentException()
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Selection<*>> kotlinSelection(
            javaSelection: Selection<*>,
            kind: BaseTableSelectionKind,
            nullable: Boolean
        ): T =
            if (kind.isTable) {
                if (nullable || kind.isNullable) {
                    KNullableTableExImpl(javaSelection as TableImplementor) as T
                } else {
                    KNonNullTableExImpl(javaSelection as TableImplementor) as T
                }
            } else {
                if (nullable || kind.isNullable) {
                    JavaToKotlinNullableExpression(javaSelection as ExpressionImplementor<T>) as T
                } else {
                    JavaToKotlinNonNullExpression(javaSelection as ExpressionImplementor<T>) as T
                }
            }
    }

    private class NonNullTable1<T1 : Selection<*>, T1Nullable : Selection<*>>(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable1<T1, T1Nullable> {

        @Suppress("UNCHECKED_CAST")
        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable1<T1>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable1<T1 : Selection<*>>(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable1<T1> {

        @Suppress("UNCHECKED_CAST")
        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable1<T1>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable2<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable2<
            T1,
            T2,
            T1Nullable,
            T2Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable2<
            T1 : Selection<*>,
            T2 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable2<
            T1,
            T2
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1, T2>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1, T2>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable2<T1, T2, T1, T2>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable2<T1, T2, T1, T2>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable3<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable3<
            T1,
            T2,
            T3,
            T1Nullable,
            T2Nullable,
            T3Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable3<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable3<
            T1,
            T2,
            T3
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable3<T1, T2, T3, T1, T2, T3>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable4<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable4<
            T1,
            T2,
            T3,
            T4,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>
        ): TT =
            weakJoin(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable4<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable4<
            T1,
            T2,
            T3,
            T4
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>
        ): TT =
            weakJoin(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable4<T1, T2, T3, T4, T1, T2, T3, T4>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable5<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>,
            T5Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable5<
            T1,
            T2,
            T3,
            T4,
            T5,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable5<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable5<
            T1,
            T2,
            T3,
            T4,
            T5
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1, T2, T3, T4, T5>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable6<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>,
            T5Nullable : Selection<*>,
            T6Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable6<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                false
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable6<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable6<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                true
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable6<*, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1, T2, T3, T4, T5, T6>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable7<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>,
            T5Nullable : Selection<*>,
            T6Nullable : Selection<*>,
            T7Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                false
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                false
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable7<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                true
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                true
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable7<*, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1, T2, T3, T4, T5, T6, T7>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable8<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>,
            T8 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>,
            T5Nullable : Selection<*>,
            T6Nullable : Selection<*>,
            T7Nullable : Selection<*>,
            T8Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                false
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                false
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                false
            )

        override val _8: T8
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._8,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[7],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable8<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>,
            T8 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                true
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                true
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                true
            )

        override val _8: T8
            get() = kotlinSelection(
                (javaTable as BaseTable8<*, *, *, *, *, *, *, *>)._8,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[7],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1, T2, T3, T4, T5, T6, T7, T8>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NonNullTable9<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>,
            T8 : Selection<*>,
            T9 : Selection<*>,
            T1Nullable : Selection<*>,
            T2Nullable : Selection<*>,
            T3Nullable : Selection<*>,
            T4Nullable : Selection<*>,
            T5Nullable : Selection<*>,
            T6Nullable : Selection<*>,
            T7Nullable : Selection<*>,
            T8Nullable : Selection<*>,
            T9Nullable : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNonNullBaseTable9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            T9,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable,
            T9Nullable
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                false
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                false
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                false
            )

        override val _8: T8
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._8,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[7],
                false
            )

        override val _9: T9
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._9,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[8],
                false
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<
                    KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }

    private class NullableTable9<
            T1 : Selection<*>,
            T2 : Selection<*>,
            T3 : Selection<*>,
            T4 : Selection<*>,
            T5 : Selection<*>,
            T6 : Selection<*>,
            T7 : Selection<*>,
            T8 : Selection<*>,
            T9 : Selection<*>
            >(
        javaTable: BaseTable
    ) : AbstractKBaseTable(javaTable), KNullableBaseTable9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            T9
            > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[0],
                true
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[1],
                true
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[2],
                true
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[3],
                true
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[4],
                true
            )

        override val _6: T6
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._6,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[5],
                true
            )

        override val _7: T7
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._7,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[6],
                true
            )

        override val _8: T8
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._8,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[7],
                true
            )

        override val _9: T9
            get() = kotlinSelection(
                (javaTable as BaseTable9<*, *, *, *, *, *, *, *, *>)._9,
                (javaTable as AbstractBaseTableSymbol).selectionLayout[8],
                true
            )

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TT : KNonNullBaseTable<*>> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>>
        ): TT =
            weakJoinImpl(targetSymbol, weakJoinType)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinLambda: KPropsWeakJoinFun<
                    KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinLambda)

        override fun <TNT : KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1, T2, T3, T4, T5, T6, T7, T8, T9>, TT>>
        ): TNT =
            weakOuterJoinImpl(targetSymbol, weakJoinType)
    }
}
