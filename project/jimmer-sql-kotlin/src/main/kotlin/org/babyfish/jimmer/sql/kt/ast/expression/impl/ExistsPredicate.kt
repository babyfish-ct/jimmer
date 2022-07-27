package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class ExistsPredicate(
    private val negative: Boolean,
    private val subQuery: KTypedSubQuery<*>
): AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        ExistsPredicate(!negative, subQuery)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (subQuery as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(if (negative) "not exists" else "exists")
        (subQuery as Ast).renderTo(builder)
    }
}