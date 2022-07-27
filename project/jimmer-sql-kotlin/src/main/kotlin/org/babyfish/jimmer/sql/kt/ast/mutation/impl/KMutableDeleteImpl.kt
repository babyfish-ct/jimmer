package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KMutableDeleteImpl<E: Any>(
    private val javaDelete: MutableDeleteImpl
): KMutableDelete<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaDelete.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>) {
        javaDelete.where(*predicates.map { it.toJavaPredicate() }.toTypedArray())
    }

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(javaDelete)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaDelete)
}