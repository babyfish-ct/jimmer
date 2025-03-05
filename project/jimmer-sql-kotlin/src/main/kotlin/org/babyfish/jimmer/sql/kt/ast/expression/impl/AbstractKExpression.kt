package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.*

internal abstract class AbstractKExpression<T: Any>: ExpressionImplementor<T>, KExpression<T>, Ast {

    private var isLowestPrecedenceUsing = false

    private var hasVirtualPredicate: Boolean? = null

    protected open fun renderChild(ast: Ast, builder: AbstractSqlBuilder<*>) {
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

    final override fun hasVirtualPredicate(): Boolean =
        hasVirtualPredicate ?: determineHasVirtualPredicate().also {
            hasVirtualPredicate = it
        }

    protected abstract fun determineHasVirtualPredicate(): Boolean

    final override fun resolveVirtualPredicate(ctx: AstContext): Ast? {
        if (!hasVirtualPredicate()) {
            return this
        }
        return onResolveVirtualPredicate(ctx)
    }

    protected abstract fun onResolveVirtualPredicate(ctx: AstContext): Ast?

    companion object {

        @JvmStatic
        protected fun hasVirtualPredicate(expression: Any?): Boolean =
            (expression is Ast && expression.hasVirtualPredicate()) ||
                (expression is MutableStatementImplementor && expression.hasVirtualPredicate())

        @JvmStatic
        protected fun hasVirtualPredicate(expressions: Collection<*>): Boolean =
            expressions.any { hasVirtualPredicate(it) }

        @JvmStatic
        protected fun <T> hasVirtualPredicate(expressions: Array<T>): Boolean =
            expressions.any { hasVirtualPredicate(it) }
    }
}

@Suppress("UNCHECKED_CAST")
internal class NonNullExpressionWrapper<T: Any>(
    val target: KNullableExpression<T>
) : AbstractKExpression<T>(),
    KNonNullExpression<T>,
    ExpressionImplementor<T> by target as ExpressionImplementor<T> {

    override fun accept(visitor: AstVisitor) {
        (target as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        (target as Ast).renderTo(builder)
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(target)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        ctx.resolveVirtualPredicate(target)
        return this
    }
}

@Suppress("UNCHECKED_CAST")
internal class NullableExpressionWrapper<T: Any>(
    val target: KNonNullExpression<T>
) : AbstractKExpression<T>(),
    KNullableExpression<T>,
    ExpressionImplementor<T> by target as ExpressionImplementor<T> {

    override fun accept(visitor: AstVisitor) {
        (target as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        (target as Ast).renderTo(builder)
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(target)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        ctx.resolveVirtualPredicate(target)
        return this
    }
}
