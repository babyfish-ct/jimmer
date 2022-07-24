package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class BinaryOperatorExpression<N: Number>(
    private val left: KExpression<N>,
    private val right: KExpression<N>
) : AbstractKExpression<N>() {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<N> =
        (left as ExpressionImplementor<N>).type

    override fun accept(visitor: AstVisitor) {
        (left as Ast).accept(visitor)
        (right as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        renderChild((left as Ast)!!, builder)
        builder.sql(" ")
        builder.sql(operator())
        builder.sql(" ")
        renderChild((right as Ast)!!, builder)
    }

    protected abstract fun operator(): String

    class NonNullPlus<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "+"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NonNullMinus<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "-"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NonNullTimes<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "*"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NonNullDiv<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "/"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NonNullRem<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNonNullExpression<N> {

        override fun operator(): String = "%"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullablePlus<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "+"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NullableMinus<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "-"

        override fun precedence(): Int = ExpressionPrecedences.PLUS
    }

    class NullableTimes<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "*"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullableDiv<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "/"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }

    class NullableRem<N: Number>(
        left: KExpression<N>,
        right: KExpression<N>
    ): BinaryOperatorExpression<N>(left, right), KNullableExpression<N> {

        override fun operator(): String = "%"

        override fun precedence(): Int = ExpressionPrecedences.TIMES
    }
}