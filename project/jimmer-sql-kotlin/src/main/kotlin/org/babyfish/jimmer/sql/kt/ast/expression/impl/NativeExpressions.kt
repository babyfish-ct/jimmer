package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.NativeBuilderImpl
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

class NativeDsl internal constructor(
    private val sql: String
) {
    private val expressions = mutableListOf<KExpression<*>>()

    private val values = mutableListOf<KExpression<*>>()

    fun <T: Any> expression(expression: KExpression<T>) {
        expressions += expression
    }

    fun <T: Any> value(value: T) {
        values += org.babyfish.jimmer.sql.kt.ast.expression.value(value)
    }

    fun parts(): List<Any> =
        NativeBuilderImpl.parts(sql, expressions, values)
}

internal abstract class AbstractNativeExpression<T: Any>(
    private val type: Class<T>,
    private var parts: List<Any>
) : AbstractKExpression<T>() {

    override fun getType(): Class<T> = type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        for (part in parts) {
            if (part is KExpression<*>) {
                (part as Ast).accept(visitor)
            }
        }
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        for (part in parts) {
            when (part) {
                is String -> builder.sql(part)
                is KExpression<*> -> renderChild(part as Ast, builder)
                else -> error("Internal bug")
            }
        }
    }

    override fun determineHasVirtualPredicate(): Boolean =
        parts.any { it is KExpression<*> && hasVirtualPredicate(it) }

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        parts = parts.map {
            if (it is KExpression<*>) {
                ctx.resolveVirtualPredicate(it)
            } else {
                it
            }
        }
        return this
    }
}

internal class NonNullNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNonNullExpression<T>

internal class NullableNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNullableExpression<T>

internal class NativePredicate private constructor(
    private val negative: Boolean,
    private var parts: List<Any>
) : AbstractKPredicate() {

    constructor(parts: List<Any>): this(false, parts)

    override fun precedence(): Int = 0

    override fun not(): Predicate =
        NativePredicate(!negative, parts)

    override fun accept(visitor: AstVisitor) {
        for (part in parts) {
            if (part is KExpression<*>) {
                (part as Ast).accept(visitor)
            }
        }
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        if (negative) {
            builder.sql("not (")
        }
        for (part in parts) {
            when (part) {
                is String -> builder.sql(part)
                is KExpression<*> -> renderChild(part as Ast, builder)
                else -> error("Internal bug")
            }
        }
        if (negative) {
            builder.sql(")")
        }
    }

    override fun determineHasVirtualPredicate(): Boolean =
        parts.any { it is KExpression<*> && hasVirtualPredicate(it) }

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        parts = parts.map {
            if (it is KExpression<*>) {
                ctx.resolveVirtualPredicate(it)
            } else {
                it
            }
        }
        return this
    }
}