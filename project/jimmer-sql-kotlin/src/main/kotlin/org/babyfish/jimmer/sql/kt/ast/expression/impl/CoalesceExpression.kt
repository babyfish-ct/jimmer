package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.runtime.SqlBuilder

abstract class Coalesce<T: Any> internal constructor(
    private val prev: Coalesce<T>?,
    internal var expression: KExpression<T>
) {
    internal fun accept(visitor: AstVisitor) {
        prev?.accept(visitor)
        (expression as Ast).accept(visitor)
    }

    internal fun renderTo(builder: SqlBuilder) {
        val prev = this.prev
        if (prev == null) {
            builder.sql("coalesce(")
        } else {
            prev.renderTo(builder)
            builder.sql(", ")
        }
        (expression as Ast).renderTo(builder)
    }

    internal fun hasVirtualPredicate(): Boolean =
        (prev?.hasVirtualPredicate() ?: false) ||
            (expression as Ast).hasVirtualPredicate()

    internal fun resolveVirtualPredicates(ctx: AstContext) {
        prev?.resolveVirtualPredicates(ctx)
        expression = ctx.resolveVirtualPredicate(expression)
    }
}

class NullableCoalesce<T: Any> internal constructor(
    prev: Coalesce<T>?,
    expression: KNullableExpression<T>
) : Coalesce<T>(prev, expression) {

    fun or(value: KNonNullExpression<T>): NonNullCoalesce<T> =
        NonNullCoalesce(this, value)

    fun or(value: T): NonNullCoalesce<T> =
        NonNullCoalesce(this, value(value))

    fun or(value: KNullableExpression<T>): NullableCoalesce<T> =
        NullableCoalesce(this, value)

    fun end(): KNullableExpression<T> =
        NullableCoalesceExpression(this)
}

class NonNullCoalesce<T: Any> internal constructor(
    prev: Coalesce<T>?,
    expression: KNonNullExpression<T>
) : Coalesce<T>(prev, expression) {

    fun end(): KNonNullExpression<T> =
        NonNullCoalesceExpression(this)
}

internal abstract class CoalesceExpression<T: Any>(
    private val coalesce: Coalesce<T>
): AbstractKExpression<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> =
        (coalesce.expression as ExpressionImplementor<T>).type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        coalesce.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            coalesce.renderTo(builder)
            builder.sql(")")
        }
    }

    override fun determineHasVirtualPredicate(): Boolean =
        coalesce.hasVirtualPredicate()

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        coalesce.resolveVirtualPredicates(ctx)
        return this
    }
}

internal class NonNullCoalesceExpression<T: Any>(
    private val coalesce: Coalesce<T>
) : CoalesceExpression<T>(coalesce), KNonNullExpression<T>

internal class NullableCoalesceExpression<T: Any>(
    private val coalesce: Coalesce<T>
) : CoalesceExpression<T>(coalesce), KNullableExpression<T>