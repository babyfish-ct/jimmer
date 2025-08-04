package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.query.MutableBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.AbstractKBaseTableImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal open class KMutableBaseQueryImpl<E: Any>(
    private val javaBaseQuery: MutableBaseQueryImpl
) : KMutableBaseQuery<E>, MutableStatementImplementor {

    override val table: KNonNullTable<E>
        get() = KNonNullTableExImpl(javaBaseQuery.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaBaseQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun where(block: () -> KNonNullPropExpression<Boolean>?) {
        where(block())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaBaseQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaBaseQuery.orderBy(*orders)
    }

    override fun orderBy(orders: List<Order?>) {
        javaBaseQuery.orderBy(*orders.toTypedArray())
    }

    override fun groupBy(vararg expressions: KExpression<*>) {
        javaBaseQuery.groupBy(*expressions.map { it as Expression<*>}.toTypedArray())
    }

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) {
        javaBaseQuery.having(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun hasVirtualPredicate(): Boolean =
        javaBaseQuery.hasVirtualPredicate()

    override fun resolveVirtualPredicate(ctx: AstContext) {
        javaBaseQuery.resolveVirtualPredicate(ctx)
    }

    override val subQueries: KSubQueries<KNonNullTable<E>> =
        KSubQueriesImpl(javaBaseQuery, table)

    override val wildSubQueries: KWildSubQueries<KNonNullTable<E>> =
        KWildSubQueriesImpl(javaBaseQuery, table)

    override val where: Where =
        Where(this)

    override val selections: KMutableBaseQuery.Selections
        get() = SelectionsImpl()

    private inner class SelectionsImpl : KMutableBaseQuery.Selections {

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> add(table: KNonNullTable<T>): KConfigurableBaseQuery.Query1<KNonNullTable<T>, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            return AbstractKConfigurableBaseQueryImpl.Query1Impl(
                javaBaseQuery.addSelect(javaTable) as ConfigurableBaseQuery<BaseTable>,
                byteArrayOf(AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_TABLE)
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> add(table: KNullableTable<T>): KConfigurableBaseQuery.Query1<KNullableTable<T>, KNullableTable<T>> {
            val javaTable = (table as KTableImplementor<*>).javaTable
            return AbstractKConfigurableBaseQueryImpl.Query1Impl(
                javaBaseQuery.addSelect(javaTable) as ConfigurableBaseQuery<BaseTable>,
                byteArrayOf(AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_TABLE)
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> add(expression: KNonNullExpression<T>): KConfigurableBaseQuery.Query1<KNonNullExpression<T>, KNullableExpression<T>> {
            return AbstractKConfigurableBaseQueryImpl.Query1Impl(
                javaBaseQuery.addSelect(expression as Expression<*>) as ConfigurableBaseQuery<BaseTable>,
                byteArrayOf(AbstractKBaseTableImpl.SELECTION_TYPE_NON_NULL_EXPRESSION)
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> add(expression: KNullableExpression<T>): KConfigurableBaseQuery.Query1<KNullableExpression<T>, KNullableExpression<T>> {
            return AbstractKConfigurableBaseQueryImpl.Query1Impl(
                javaBaseQuery.addSelect(expression as Expression<*>) as ConfigurableBaseQuery<BaseTable>,
                byteArrayOf(AbstractKBaseTableImpl.SELECTION_TYPE_NULLABLE_EXPRESSION)
            )
        }
    }
}