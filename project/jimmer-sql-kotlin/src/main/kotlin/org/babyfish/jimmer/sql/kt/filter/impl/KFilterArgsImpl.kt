package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.association.meta.AssociationType
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
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl
import java.lang.IllegalStateException
import kotlin.reflect.KClass

internal class KFilterArgsImpl<E: Any>(
    val javaStatement: AbstractMutableStatementImpl,
    javaTable: TableImplementor<E>,
    private val filteredType: ImmutableType
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

    override val subQueries: KSubQueries<E> by lazy {
        KSubQueriesImpl(javaStatement, subQueryParentTable())
    }

    override val wildSubQueries: KWildSubQueries<E> by lazy {
        KWildSubQueriesImpl(javaStatement, )
    }

    private fun subQueryParentTable() : KNonNullTableEx<E>? {
        val tableImplementor = (table as KTableImplementor<*>).javaTable
        val associationType = tableImplementor.immutableType as? AssociationType ?: return null
        return when (filteredType) {
            associationType.sourceType -> table.join("source")
            associationType.targetType -> table.join("target")
            else -> throw IllegalStateException(
                "The association property \"$associationType\" is illegal," +
                    "either source type nor target type is not filtered type \"$filteredType\""
            )
        }
    }

    companion object {
        @JvmStatic
        private val JOIN_DISABLED_REASON = "it is not allowed by cacheable filter"
    }
}