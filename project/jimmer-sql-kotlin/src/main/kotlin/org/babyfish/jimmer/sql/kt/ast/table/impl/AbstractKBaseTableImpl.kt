package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass

internal abstract class AbstractKBaseTableImpl(
    internal val javaTable: BaseTable,
    protected val selectionTypes: ByteArray
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

    companion object {

        val SELECTION_TYPE_NON_NULL_TABLE: Byte = (0 or 0).toByte()
        val SELECTION_TYPE_NULLABLE_TABLE: Byte = (2 or 0).toByte()
        val SELECTION_TYPE_NON_NULL_EXPRESSION: Byte = (0 or 1).toByte()
        val SELECTION_TYPE_NULLABLE_EXPRESSION: Byte = (2 or 1).toByte()

        fun of(baseTable: BaseTable, selectionTypes: ByteArray) : AbstractKBaseTableImpl =
            when (selectionTypes.size) {
                1 -> NonNullTable1<
                    Selection<*>,
                    Selection<*>
                >(baseTable, selectionTypes)
                2 -> NonNullTable2<
                    Selection<*>,
                    Selection<*>,
                    Selection<*>,
                    Selection<*>
                >(baseTable, selectionTypes)
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
            val mask = if (nullable) {
                selectionType.toInt() or 2
            } else {
                selectionType
            }.toByte()
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
        javaTable: BaseTable,
        selectionTypes: ByteArray
    ) : AbstractKBaseTableImpl(javaTable, selectionTypes), KNonNullBaseTable1<T1, T1Nullable> {

        @Suppress("UNCHECKED_CAST")
        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable1<T1>)._1,
                selectionTypes[0],
                false
            )

        override fun <TT : KBaseTable> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            joinType: JoinType,
            weakJoinLambda: KPropsWeakJoinFun<KNonNullBaseTable1<T1, T1Nullable>, TT>
        ): TT {
            TODO("Not yet implemented")
        }

        @Suppress("UNCHECKED_CAST")
        override fun <TT : KBaseTable> weakJoin(
            targetSymbol: KBaseTableSymbol<TT>,
            joinType: JoinType,
            weakJoinType: KClass<out KPropsWeakJoin<KNonNullBaseTable1<T1, T1Nullable>, TT>>
        ): TT {
            val handle = WeakJoinHandle.of(weakJoinType.java)
            val javaJoinedTable = BaseTableSymbols.of(targetSymbol.baseTable as BaseTableSymbol?, javaTable, handle, joinType, null)
            return of(javaJoinedTable, (targetSymbol.baseTable as AbstractKBaseTableImpl).selectionTypes) as TT
        }
    }

    private class NonNullTable2<
        T1: Selection<*>,
        T2: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>
    >(
        javaTable: BaseTable,
        selectionTypes: ByteArray
    ) : AbstractKBaseTableImpl(javaTable, selectionTypes), KNonNullBaseTable2<
        T1,
        T2,
        T1Nullable,
        T2Nullable
    > {

        override val _1: T1
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._1,
                selectionTypes[0],
                false
            )

        override val _2: T2
            get() = kotlinSelection(
                (javaTable as BaseTable2<*, *>)._2,
                selectionTypes[1],
                false
            )
    }
}