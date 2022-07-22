package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KTable

interface KFilterable<E> {

    val table: KTable<E>

    fun where(vararg predicates: KNonNullExpression<Boolean>)
}