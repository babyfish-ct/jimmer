package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class AbstractKExpression<T: Any>(

): KExpression<T>, Ast {

    private var isLowestPrecedenceUsing = false

    abstract val precedence: Int

    protected open fun renderChild(ast: Ast, builder: SqlBuilder) {
        if (isLowestPrecedenceUsing ||
            ast !is AbstractKExpression<*> ||
            ast.precedence <= precedence) {
            ast.renderTo(builder)
        } else {
            builder.sql("(")
            ast.renderTo(builder)
            builder.sql(")")
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
