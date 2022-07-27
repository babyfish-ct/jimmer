package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl
import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KFilterDslImpl<E: Any>(
    private val javaQuery: AbstractMutableQueryImpl,
    private val keys: Collection<Any>
) : KFilterDsl<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaQuery.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>) {
        javaQuery.where(*predicates.map { it.toJavaPredicate() }.toTypedArray())
    }

    override fun orderBy(expression: KExpression<*>, orderMode: OrderMode, nullOrderMode: NullOrderMode) {
        javaQuery.orderBy(expression as Expression<*>, orderMode, nullOrderMode)
    }

    override fun groupBy(vararg expressions: KExpression<*>) {
        javaQuery.groupBy(*expressions.map { it as Expression<*> }.toTypedArray())
    }

    override fun having(vararg predicates: KNonNullExpression<Boolean>) {
        javaQuery.having(*predicates.map { it.toJavaPredicate() }.toTypedArray())
    }

    override val subQueries: KSubQueries<E> by lazy {
        KSubQueriesImpl(javaQuery)
    }

    override val wildSubQueries: KWildSubQueries<E> by lazy {
        KWildSubQueriesImpl(javaQuery)
    }
}