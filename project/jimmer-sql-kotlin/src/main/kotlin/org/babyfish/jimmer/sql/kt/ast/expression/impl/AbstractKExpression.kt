package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class AbstractKExpression<T: Any>: ExpressionImplementor<T>, KExpression<T>, Ast {

    private var isLowestPrecedenceUsing = false

    protected open fun renderChild(ast: Ast, builder: SqlBuilder) {
        if (isLowestPrecedenceUsing ||
            ast !is ExpressionImplementor<*> ||
            ast.precedence() <= precedence()) {
            ast.renderTo(builder)
        } else {
            builder.sql("(").space('\n')
            ast.renderTo(builder)
            builder.space('\n').sql(")")
        }
    }

    protected open fun usingLowestPrecedence(block: () -> Unit) {
        if (isLowestPrecedenceUsing) {
            block()
        } else {
            isLowestPrecedenceUsing = true
            try {
                block()
            } finally {
                isLowestPrecedenceUsing = false
            }
        }
    }
}

internal class NonNullExpressionWrapper<T: Any>(
    val target: KNullableExpression<T>
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> =
        (target as ExpressionImplementor<T>).type

    @Suppress("UNCHECKED_CAST")
    override fun precedence(): Int =
        (target as ExpressionImplementor<T>).precedence()

    override fun accept(visitor: AstVisitor) {
        (target as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        (target as Ast).renderTo(builder)
    }
}

internal class NullableExpressionWrapper<T: Any>(
    val target: KNonNullExpression<T>
) : AbstractKExpression<T>(), KNullableExpression<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> =
        (target as ExpressionImplementor<T>).type

    @Suppress("UNCHECKED_CAST")
    override fun precedence(): Int =
        (target as ExpressionImplementor<T>).precedence()

    override fun accept(visitor: AstVisitor) {
        (target as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        (target as Ast).renderTo(builder)
    }
}