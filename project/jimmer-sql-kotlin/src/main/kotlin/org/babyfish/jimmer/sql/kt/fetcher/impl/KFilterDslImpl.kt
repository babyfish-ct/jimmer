package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KFilterDslImpl<E: Any>(
    private val javaQuery: AbstractMutableQueryImpl,
    javaTable: TableImplementor<E>,
    val keys: Collection<Any>
) : KFieldFilterDsl<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaTable)

    override val where: Where by lazy {
        Where(this)
    }

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun where(block: () -> KNonNullPropExpression<Boolean>?) {
        where(block())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaQuery.orderBy(*orders)
    }

    override fun orderBy(orders: List<Order?>) {
        javaQuery.orderBy(orders)
    }

    override val subQueries: KSubQueries<KNonNullTable<E>> by lazy {
        KSubQueriesImpl(javaQuery)
    }

    override val wildSubQueries: KWildSubQueries<KNonNullTable<E>> by lazy {
        KWildSubQueriesImpl(javaQuery)
    }
}