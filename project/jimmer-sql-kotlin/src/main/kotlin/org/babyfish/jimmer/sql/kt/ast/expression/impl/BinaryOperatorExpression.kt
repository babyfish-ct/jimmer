package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class BinaryOperatorExpression<N: Any>(
    private var left: KExpression<N>,
    private var right: KExpression<N>
) : AbstractKExpression<N>() {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<N> =
        (left as ExpressionImplementor<N>).type

    override fun accept(visitor: AstVisitor) {
        (left as Ast).accept(visitor)
        (right as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        renderChild((left as Ast), builder)
        builder.sql(" ")
        builder.sql(operator())
        builder.sql(" ")
        renderChild((right as Ast), builder)
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(left) || hasVirtualPredicate(right)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        left = ctx.resolveVirtualPredicate(left)
        right = ctx.resolveVirtualPredicate(right)
        return this
    }

    protected abstract fun operator(): String

    class NonNullPlus<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "+"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NonNullMinus<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "-"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NonNullTimes<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "*"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NonNullDiv<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "/"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NonNullRem<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "%"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullablePlus<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "+"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NullableMinus<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "-"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NullableTimes<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "*"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullableDiv<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "/"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullableRem<N: Any>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "%"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }
}