package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class NullityPredicate(
    protected val expression: KExpression<*>
) : AbstractKPredicate() {

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) =
        (expression as Ast).accept(visitor)

    override fun renderTo(builder: SqlBuilder) {
        (expression as Ast).renderTo(builder)
        if (isNegative()) {
            builder.sql(" is not null")
        } else {
            builder.sql(" is null")
        }
    }

    protected abstract fun isNegative(): Boolean
}

internal class IsNullPredicate(
    expression: KExpression<*>
) : NullityPredicate(expression) {

    override fun isNegative(): Boolean = false

    override fun not(): AbstractKPredicate =
        IsNotNullPredicate(expression)
}

internal class IsNotNullPredicate(
    expression: KExpression<*>
) : NullityPredicate(expression) {

    override fun isNegative(): Boolean = true

    override fun not(): AbstractKPredicate =
        IsNullPredicate(expression)
}
