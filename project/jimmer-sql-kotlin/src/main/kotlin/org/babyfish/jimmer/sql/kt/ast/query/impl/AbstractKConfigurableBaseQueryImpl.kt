package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.ast.table.impl.AbstractKBaseTableImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

internal abstract class AbstractKConfigurableBaseQueryImpl<T: KNonNullBaseTable<*>>(
    internal val javaBaseQuery: ConfigurableBaseQuery<out BaseTable>,
    protected val selectionTypes: ByteArray
) : KConfigurableBaseQuery<T> {

    @Suppress("UNCHECKED_CAST")
    override fun asBaseTable(): KBaseTableSymbol<T> =
        KBaseTableSymbol(
            AbstractKBaseTableImpl.nonNull(
                (javaBaseQuery as TypedBaseQueryImplementor<*>)
                    .asBaseTable(selectionTypes, false)
            ) as T
        )

    @Suppress("UNCHECKED_CAST")
    override fun asCteBaseTable(): KBaseTableSymbol<T> =
        KBaseTableSymbol(
            AbstractKBaseTableImpl.nonNull(
                (javaBaseQuery as TypedBaseQueryImplementor<*>)
                    .asBaseTable(selectionTypes, true)
            ) as T
        )

    internal class Query1Impl<
        T1: Selection<*>,
        T1Nullable: Selection<*>
    >(
        javaQuery: ConfigurableBaseQuery<BaseTable>,
        selectionTypes: ByteArray
    ) : AbstractKConfigurableBaseQueryImpl<
        KNonNullBaseTable1<T1, T1Nullable>
    >(javaQuery, selectionTypes), KConfigurableBaseQuery.Query1<T1, T1Nullable> {

        override fun <T : Any> add(
            table: KNonNullTable<T>
        ): KConfigurableBaseQuery.Query2<
            T1,
            KNonNullTable<T>,
            T1Nullable,
            KNullableTable<T>
        > {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery1 = javaBaseQuery as ConfigurableBaseQuery.Query1<*>
            return Query2Impl(
                javaQuery1.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_TABLE)
            )
        }

        override fun <T : Any> add(table: KNullableTable<T>): KConfigurableBaseQuery.Query2<T1, KNullableTable<T>, T1Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery1 = javaBaseQuery as ConfigurableBaseQuery.Query1<*>
            return Query2Impl(
                javaQuery1.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_TABLE)
            )
        }

        override fun <T : Any> add(expression: KNonNullExpression<T>): KConfigurableBaseQuery.Query2<T1, KNonNullExpression<T>, T1Nullable, KNullableExpression<T>> {
            val javaQuery1 = javaBaseQuery as ConfigurableBaseQuery.Query1<*>
            return Query2Impl(
                javaQuery1.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_EXPRESSION)
            )
        }

        override fun <T : Any> add(expression: KNullableExpression<T>): KConfigurableBaseQuery.Query2<T1, KNullableExpression<T>, T1Nullable, KNullableExpression<T>> {
            val javaQuery1 = javaBaseQuery as ConfigurableBaseQuery.Query1<*>
            return Query2Impl(
                javaQuery1.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_EXPRESSION)
            )
        }
    }

    private class Query2Impl<
        T1: Selection<*>,
        T2: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
    >(
        javaQuery: ConfigurableBaseQuery<out BaseTable>,
        selectionTypes: ByteArray
    ) : AbstractKConfigurableBaseQueryImpl<
        KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>
    >(javaQuery, selectionTypes), KConfigurableBaseQuery.Query2<T1, T2, T1Nullable, T2Nullable> {

        override fun <T : Any> add(table: KNonNullTable<T>): KConfigurableBaseQuery.Query3<T1, T2, KNonNullTable<T>, T1Nullable, T2Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery2 = javaBaseQuery as ConfigurableBaseQuery.Query2<*, *>
            return Query3Impl(
                javaQuery2.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_TABLE)
            )
        }

        override fun <T : Any> add(table: KNullableTable<T>): KConfigurableBaseQuery.Query3<T1, T2, KNullableTable<T>, T1Nullable, T2Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery2 = javaBaseQuery as ConfigurableBaseQuery.Query2<*, *>
            return Query3Impl(
                javaQuery2.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_TABLE)
            )
        }

        override fun <T : Any> add(expression: KNonNullExpression<T>): KConfigurableBaseQuery.Query3<T1, T2, KNonNullExpression<T>, T1Nullable, T2Nullable, KNullableExpression<T>> {
            val javaQuery2 = javaBaseQuery as ConfigurableBaseQuery.Query2<*, *>
            return Query3Impl(
                javaQuery2.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_EXPRESSION)
            )
        }

        override fun <T : Any> add(expression: KNullableExpression<T>): KConfigurableBaseQuery.Query3<T1, T2, KNullableExpression<T>, T1Nullable, T2Nullable, KNullableExpression<T>> {
            val javaQuery2 = javaBaseQuery as ConfigurableBaseQuery.Query2<*, *>
            return Query3Impl(
                javaQuery2.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_EXPRESSION)
            )
        }
    }

    private class Query3Impl<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
    >(
        javaQuery: ConfigurableBaseQuery<out BaseTable>,
        selectionTypes: ByteArray
    ) : AbstractKConfigurableBaseQueryImpl<
        KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>
    >(javaQuery, selectionTypes), KConfigurableBaseQuery.Query3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable> {

        override fun <T : Any> add(table: KNonNullTable<T>): KConfigurableBaseQuery.Query4<T1, T2, T3, KNonNullTable<T>, T1Nullable, T2Nullable, T3Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery3 = javaBaseQuery as ConfigurableBaseQuery.Query3<*, *, *>
            return Query4Impl(
                javaQuery3.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_TABLE)
            )
        }

        override fun <T : Any> add(table: KNullableTable<T>): KConfigurableBaseQuery.Query4<T1, T2, T3, KNullableTable<T>, T1Nullable, T2Nullable, T3Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery3 = javaBaseQuery as ConfigurableBaseQuery.Query3<*, *, *>
            return Query4Impl(
                javaQuery3.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_TABLE)
            )
        }

        override fun <T : Any> add(expression: KNonNullExpression<T>): KConfigurableBaseQuery.Query4<T1, T2, T3, KNonNullExpression<T>, T1Nullable, T2Nullable, T3Nullable, KNullableExpression<T>> {
            val javaQuery3 = javaBaseQuery as ConfigurableBaseQuery.Query3<*, *, *>
            return Query4Impl(
                javaQuery3.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_EXPRESSION)
            )
        }

        override fun <T : Any> add(expression: KNullableExpression<T>): KConfigurableBaseQuery.Query4<T1, T2, T3, KNullableExpression<T>, T1Nullable, T2Nullable, T3Nullable, KNullableExpression<T>> {
            val javaQuery3 = javaBaseQuery as ConfigurableBaseQuery.Query3<*, *, *>
            return Query4Impl(
                javaQuery3.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_EXPRESSION)
            )
        }
    }

    private class Query4Impl<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
    >(
        javaQuery: ConfigurableBaseQuery<out BaseTable>,
        selectionTypes: ByteArray
    ) : AbstractKConfigurableBaseQueryImpl<
        KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>
    >(javaQuery, selectionTypes), KConfigurableBaseQuery.Query4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable> {

        override fun <T : Any> add(table: KNonNullTable<T>): KConfigurableBaseQuery.Query5<T1, T2, T3, T4, KNonNullTable<T>, T1Nullable, T2Nullable, T3Nullable, T4Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery4 = javaBaseQuery as ConfigurableBaseQuery.Query4<*, *, *, *>
            return Query5Impl(
                javaQuery4.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_TABLE)
            )
        }

        override fun <T : Any> add(table: KNullableTable<T>): KConfigurableBaseQuery.Query5<T1, T2, T3, T4, KNullableTable<T>, T1Nullable, T2Nullable, T3Nullable, T4Nullable, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            val javaQuery4 = javaBaseQuery as ConfigurableBaseQuery.Query4<*, *, *, *>
            return Query5Impl(
                javaQuery4.addSelect(javaTable),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_TABLE)
            )
        }

        override fun <T : Any> add(expression: KNonNullExpression<T>): KConfigurableBaseQuery.Query5<T1, T2, T3, T4, KNonNullExpression<T>, T1Nullable, T2Nullable, T3Nullable, T4Nullable, KNullableExpression<T>> {
            val javaQuery4 = javaBaseQuery as ConfigurableBaseQuery.Query4<*, *, *, *>
            return Query5Impl(
                javaQuery4.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_EXPRESSION)
            )
        }

        override fun <T : Any> add(expression: KNullableExpression<T>): KConfigurableBaseQuery.Query5<T1, T2, T3, T4, KNullableExpression<T>, T1Nullable, T2Nullable, T3Nullable, T4Nullable, KNullableExpression<T>> {
            val javaQuery4 = javaBaseQuery as ConfigurableBaseQuery.Query4<*, *, *, *>
            return Query5Impl(
                javaQuery4.addSelect(expression as Expression<*>),
                AbstractKBaseTableImpl.selectionTypes(selectionTypes, AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_EXPRESSION)
            )
        }
    }

    private class Query5Impl<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
    >(
        javaQuery: ConfigurableBaseQuery<out BaseTable>,
        selectionTypes: ByteArray
    ) : AbstractKConfigurableBaseQueryImpl<
        KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>
    >(javaQuery, selectionTypes), KConfigurableBaseQuery.Query5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable> {

    }
}