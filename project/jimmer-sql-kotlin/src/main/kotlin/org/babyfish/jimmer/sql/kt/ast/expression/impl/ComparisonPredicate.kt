package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class ComparisonPredicate(
    private val left: KExpression<*>,
    private val right: KExpression<*>
) : AbstractKPredicate() {

    override fun precedence(): Int =
        ExpressionPrecedences.COMPARISON

    override fun accept(visitor: AstVisitor) {
        (left as Ast).accept(visitor)
        (right as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        renderChild((left as Ast), builder)
        builder.sql(" ")
        builder.sql(operator())
        builder.sql(" ")
        renderChild((right as Ast), builder)
    }

    protected abstract fun operator(): String

    class Eq(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "="
    }

    class Ne(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<>"
    }

    class Lt(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<"
    }

    class Le(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<="
    }

    class Gt(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = ">"
    }

    class Ge(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = ">="
    }
}