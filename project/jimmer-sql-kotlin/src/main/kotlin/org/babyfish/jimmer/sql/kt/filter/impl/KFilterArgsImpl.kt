package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.Sortable
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KFilterArgsImpl<E: Any>(
    val javaStatement: AbstractMutableStatementImpl,
    javaTable: TableImplementor<E>
) : KFilterArgs<E> {

    override val table: KNonNullProps<E> =
        KNonNullTableExImpl(javaTable, JOIN_DISABLED_REASON)

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaStatement.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        (javaStatement as? Sortable)?.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderByIf(condition: Boolean, vararg expressions: KExpression<*>?) {
        (javaStatement as? Sortable)?.orderByIf(condition, *expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        (javaStatement as? Sortable)?.orderBy(*orders)
    }

    override fun orderByIf(condition: Boolean, vararg orders: Order?) {
        (javaStatement as? Sortable)?.orderByIf(condition, *orders)
    }

    override fun orderBy(orders: List<Order?>) {
        (javaStatement as? Sortable)?.orderBy(orders)
    }

    override fun orderByIf(condition: Boolean, orders: List<Order?>) {
        (javaStatement as? Sortable)?.orderByIf(condition, orders)
    }

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(javaStatement)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaStatement)

    companion object {
        @JvmStatic
        private val JOIN_DISABLED_REASON = "it is not allowed by cacheable filter"
    }
}