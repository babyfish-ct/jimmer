package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.base.AbstractBaseTableSymbol
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols
import org.babyfish.jimmer.sql.ast.impl.table.JWeakJoinLambdaFactory
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3
import org.babyfish.jimmer.sql.ast.table.base.BaseTable4
import org.babyfish.jimmer.sql.ast.table.base.BaseTable5
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass

internal abstract class AbstractKBaseTableImpl(
    internal val javaTable: BaseTable
) : KBaseTable {

    override fun hashCode(): Int {
        return javaTable.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AbstractKBaseTableImpl) {
            return false
        }
        return javaTable == other.javaTable
    }

    override fun toString(): String {
        return "K$javaTable"
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST: KBaseTable, TT : KNonNullBaseTable<*>> weakJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<ST, TT>
    ): TT {
        val handle = createPropsWeakJoinHandle(this::class.java, targetSymbol::class.java, weakJoinLambda)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTableImpl).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.INNER,
            null
        )
        return nonNull(javaJoinedTable) as TT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST: KBaseTable, TT : KNonNullBaseTable<*>> weakJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<ST, TT>>
    ): TT {
        val handle = WeakJoinHandle.of(weakJoinType.java)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTableImpl).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.INNER,
            null
        )
        return nonNull(javaJoinedTable) as TT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST: KBaseTable, TNT: KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinLambda: KPropsWeakJoinFun<ST, TT>
    ): TNT {
        val handle = createPropsWeakJoinHandle(this::class.java, targetSymbol::class.java, weakJoinLambda)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTableImpl).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.LEFT,
            null
        )
        return nullable(javaJoinedTable) as TNT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <ST: KBaseTable, TNT: KNullableBaseTable, TT : KNonNullBaseTable<TNT>> weakOuterJoinImpl(
        targetSymbol: KBaseTableSymbol<TT>,
        weakJoinType: KClass<out KPropsWeakJoin<ST, TT>>
    ): TNT {
        val handle = WeakJoinHandle.of(weakJoinType.java)
        val javaJoinedTable = BaseTableSymbols.of(
            (targetSymbol.baseTable as AbstractKBaseTableImpl).javaTable as BaseTableSymbol?,
            javaTable,
            handle,
            JoinType.LEFT,
            null
        )
        return nullable(javaJoinedTable) as TNT
    }

    companion object {

        val SELECTION_TYPE_NON_NULL_TABLE: Byte = (0 or 0).toByte()
        val SELECTION_TYPE_NULLABLE_TABLE: Byte = (2 or 0).toByte()
        val SELECTION_TYPE_NON_NULL_EXPRESSION: Byte = (0 or 1).toByte()
        val SELECTION_TYPE_NULLABLE_EXPRESSION: Byte = (2 or 1).toByte()

        fun nonNull(baseTable: BaseTable) : AbstractKBaseTableImpl =
            when ((baseTable as BaseTableSymbol).selections.size) {
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
                else -> throw IllegalArgumentException()
            }

        fun nullable(baseTable: BaseTable) : AbstractKBaseTableImpl =
            when ((baseTable as BaseTableSymbol).selections.size) {
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
                else -> throw IllegalArgumentException()
            }

        fun selectionTypes(prev: ByteArray?, value: Byte): ByteArray =
            if (prev !== null) {
                prev + value
            } else {
                byteArrayOf(value)
            }

        @Suppress("UNCHECKED_CAST")
        fun <T: Selection<*>> kotlinSelection(
            javaSelection: Selection<*>,
            selectionType: Byte,
            nullable: Boolean
        ): T {
            val mask =
                if (nullable) {
                    (selectionType.toInt() or 2).toByte()
                } else {
                    selectionType
                }
            return when (mask) {
                SELECTION_TYPE_NON_NULL_TABLE ->
                    KNonNullTableExImpl(javaSelection as TableImplementor) as T
                SELECTION_TYPE_NULLABLE_TABLE ->
                    KNonNullTableExImpl(javaSelection as TableImplementor) as T
                SELECTION_TYPE_NON_NULL_EXPRESSION ->
                    JavaToKotlinNonNullExpression(javaSelection as ExpressionImplementor<T>) as T
                SELECTION_TYPE_NULLABLE_EXPRESSION ->
                    JavaToKotlinNullableExpression(javaSelection as ExpressionImplementor<T>) as T
                else ->
                    throw IllegalArgumentException("Illegal mask $mask")
            }
        }
    }

    private class NonNullTable1<T1: Selection<*>, T1Nullable: Selection<*>>(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNonNullBaseTable1<T1, T1Nullable> {

        @Suppress("UNCHECKED_CAST")
        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable1<T1>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
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

    private class NullableTable1<T1: Selection<*>>(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNullableBaseTable1<T1> {

        @Suppress("UNCHECKED_CAST")
        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable1<T1>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
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
        T1: Selection<*>,
        T2: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNonNullBaseTable2<
        T1,
        T2,
        T1Nullable,
        T2Nullable
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
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
        T1: Selection<*>,
        T2: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNullableBaseTable2<
        T1,
        T2
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
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
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNonNullBaseTable3<
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
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
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
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>
        >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNullableBaseTable3<
        T1,
        T2,
        T3
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable3<*, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
                false
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
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNonNullBaseTable4<
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
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[3],
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
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNullableBaseTable4<
        T1,
        T2,
        T3,
        T4
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable4<*, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[3],
                false
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
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNonNullBaseTable5<
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
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[4],
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
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>
    >(
        javaTable: BaseTable
    ) : AbstractKBaseTableImpl(javaTable), KNullableBaseTable5<
        T1,
        T2,
        T3,
        T4,
        T5
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._1,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._2,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[1],
                false
            )

        override val _3: T3
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._3,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[2],
                false
            )

        override val _4: T4
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._4,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[3],
                false
            )

        override val _5: T5
            get() = kotlinSelection(
                (javaTable as BaseTable5<*, *, *, *, *>)._5,
                (javaTable as AbstractBaseTableSymbol).kotlinSelectionTypes[4],
                false
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
}