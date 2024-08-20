package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression

internal abstract class ComparisonPredicate(
    protected var left: KExpression<*>,
    protected var right: KExpression<*>
) : AbstractKPredicate() {

    init {
        LiteralExpression.bind(left, right)
        LiteralExpression.bind(right, left)
    }

    override fun precedence(): Int =
        ExpressionPrecedences.COMPARISON

    override fun accept(visitor: AstVisitor) {
        (left as Ast).accept(visitor)
        (right as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        ComparisonPredicates.renderCmp(
            operator(),
            left as Expression<*>,
            right as Expression<*>,
            builder.assertSimple()
        )
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(left) || hasVirtualPredicate(right)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        left = ctx.resolveVirtualPredicate(left)
        right = ctx.resolveVirtualPredicate(right)
        return this
    }

    protected abstract fun operator(): String

    class Eq(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "="
        override fun not(): AbstractKPredicate = Ne(left, right)
    }

    class Ne(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<>"
        override fun not(): AbstractKPredicate = Eq(left, right)
    }

    class Lt(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<"
        override fun not(): AbstractKPredicate = Ge(left, right)
    }

    class Le(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = "<="
        override fun not(): AbstractKPredicate = Gt(left, right)
    }

    class Gt(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = ">"
        override fun not(): AbstractKPredicate = Le(left, right)
    }

    class Ge(left: KExpression<*>, right: KExpression<*>) :
        ComparisonPredicate(left, right) {
        override fun operator(): String = ">="
        override fun not(): AbstractKPredicate = Lt(left, right)
    }
}