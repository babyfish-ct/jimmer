package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class AbstractUnaryMinisExpression<N: Number>(
    expression: KExpression<N>
) : AbstractKExpression<N>() {

    protected var _expr: KExpression<N> = expression

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(_expr)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        this._expr = ctx.resolveVirtualPredicate(_expr)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<N> =
        (_expr as ExpressionImplementor<N>).type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (_expr as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sql("-")
        (_expr as Ast).renderTo(builder)
    }
}

internal class NonNullUnaryMinisExpression<N: Number>(
    expression: KNonNullExpression<N>
) : AbstractUnaryMinisExpression<N>(expression), KNonNullExpression<N> {

    val expression: KNonNullExpression<N> =
        _expr as KNonNullExpression<N>
}

internal class NullableUnaryMinisExpression<N: Number>(
    expression: KNullableExpression<N>
) : AbstractUnaryMinisExpression<N>(expression), KNullableExpression<N> {

    val expression: KNullableExpression<N> =
        _expr as KNullableExpression<N>
}