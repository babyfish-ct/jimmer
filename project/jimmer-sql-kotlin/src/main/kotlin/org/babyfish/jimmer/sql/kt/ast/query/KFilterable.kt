package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

interface KFilterable<E: Any> {

    val table: KNonNullTable<E>

    fun where(vararg predicates: KNonNullExpression<Boolean>)
}