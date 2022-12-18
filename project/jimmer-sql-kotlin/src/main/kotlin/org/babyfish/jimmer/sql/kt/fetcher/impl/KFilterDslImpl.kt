package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KFilterDslImpl<E: Any>(
    private val javaQuery: AbstractMutableQueryImpl,
    private val keys: Collection<Any>
) : KFieldFilterDsl<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaQuery.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderByIf(condition: Boolean, vararg expressions: KExpression<*>?) {
        javaQuery.orderByIf(condition, *expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaQuery.orderBy(*orders)
    }

    override fun orderByIf(condition: Boolean, vararg orders: Order?) {
        javaQuery.orderByIf(condition, *orders)
    }

    override val subQueries: KSubQueries<E> by lazy {
        KSubQueriesImpl(javaQuery)
    }

    override val wildSubQueries: KWildSubQueries<E> by lazy {
        KWildSubQueriesImpl(javaQuery)
    }
}