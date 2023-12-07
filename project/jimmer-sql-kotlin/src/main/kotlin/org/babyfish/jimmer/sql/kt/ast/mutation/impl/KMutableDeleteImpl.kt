package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KMutableDeleteImpl<E: Any>(
    private val javaDelete: MutableDeleteImpl
): KMutableDelete<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaDelete.getTable())

    override val where: Where =
        Where(this)

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaDelete.where(*predicates.map { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun disableDissociation() {
        javaDelete.disableDissociation()
    }

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(javaDelete)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaDelete)
}