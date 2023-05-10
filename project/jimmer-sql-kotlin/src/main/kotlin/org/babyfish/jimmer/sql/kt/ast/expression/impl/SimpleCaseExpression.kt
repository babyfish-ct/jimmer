package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KClass

class SimpleCaseStarter<T: Any> internal constructor(
    private val start: KExpression<T>
) {
    fun <R: Any> match(
        condValue: KNonNullExpression<T>,
        value: KNonNullExpression<R>
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(start, condValue, value))

    fun <R: Any> match(
        condValue: KNonNullExpression<T>,
        value: R
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(start, condValue, value(value)))

    fun <R: Any> match(
        condValue: KNonNullExpression<T>,
        value: KNullableExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(start, condValue, value))

    fun <R: Any> match(
        condValue: T,
        value: KNonNullExpression<R>
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(start, value(condValue), value))

    fun <R: Any> match(
        condValue: T,
        value: R
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(start, value(condValue), value(value)))

    fun <R: Any> match(
        condValue: T,
        value: KNullableExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(start, value(condValue), value))
}

class NonNullSimpleCase<T: Any, R: Any> internal constructor(
    private val match: SimpleMatch<T, R>
) {

    fun match(
        condValue: KNonNullExpression<T>,
        value: KNonNullExpression<R>
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(match, condValue, value))

    fun match(
        condValue: KNonNullExpression<T>,
        value: R
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(match, condValue, value(value)))

    fun match(
        condValue: KNonNullExpression<T>,
        value: KNullableExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, condValue, value))

    fun match(
        condValue: T,
        value: KNonNullExpression<R>
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(match, value(condValue), value))

    fun match(
        condValue: T,
        value: R
    ): NonNullSimpleCase<T, R> =
        NonNullSimpleCase(SimpleMatch(match, value(condValue), value(value)))

    fun match(
        condValue: T,
        value: KNullableExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, value(condValue), value))

    fun otherwise(value: KNonNullExpression<R>): KNonNullExpression<R> =
        NonNullSimpleCaseExpression(match, value)

    fun otherwise(value: R): KNonNullExpression<R> =
        NonNullSimpleCaseExpression(match, value(value))

    fun otherwise(value: KNullableExpression<R>): KNullableExpression<R> =
        NullableSimpleCaseExpression(match, value)

    @Suppress("UNCHECKED_CAST")
    fun otherwise(): KNullableExpression<R> =
        NullableSimpleCaseExpression(
            match,
            nullValue((match.value as AbstractKExpression<*>).type.kotlin as KClass<R>)
        )
}

class NullableSimpleCase<T: Any, R: Any> internal constructor(
    private val match: SimpleMatch<T, R>
) {
    fun match(
        condValue: KNonNullExpression<T>,
        value: KExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, condValue, value))

    fun match(
        condValue: KNonNullExpression<T>,
        value: R
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, condValue, value(value)))

    fun match(
        condValue: T,
        value: KExpression<R>
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, value(condValue), value))

    fun match(
        condValue: T,
        value: R
    ): NullableSimpleCase<T, R> =
        NullableSimpleCase(SimpleMatch(match, value(condValue), value(value)))

    fun otherwise(value: KExpression<R>): KNullableExpression<R> =
        NullableSimpleCaseExpression(match, value)

    @Suppress("UNCHECKED_CAST")
    fun otherwise(value: R? = null): KNullableExpression<R> =
        if (value !== null) {
            NullableSimpleCaseExpression(match, value(value))
        } else {
            NullableSimpleCaseExpression(
                match,
                nullValue((match.value as AbstractKExpression<*>).type.kotlin as KClass<R>)
            )
        }
}

internal data class SimpleMatch<T: Any, R: Any>(
    val startExpression: KExpression<T>,
    val prev: SimpleMatch<T, R>?,
    val condValue: KExpression<T>?,
    val value: KExpression<R>
) {
    constructor(startExpression: KExpression<T>, condValue: KExpression<T>, value: KExpression<R>):
        this(startExpression, null, condValue, value)

    constructor(prev: SimpleMatch<T, R>, condValue: KExpression<T>, value: KExpression<R>):
        this(prev.startExpression, prev, condValue, value)

    fun accept(visitor: AstVisitor) {
        val prev = this.prev
        if (prev === null) {
            (startExpression as Ast).accept(visitor)
        } else {
            prev.accept(visitor)
        }
        (condValue as Ast).accept(visitor)
        (value as Ast).accept(visitor)
    }

    fun renderTo(builder: SqlBuilder) {
        val prev = this.prev
        if (prev === null) {
            builder.sql("case ")
            (startExpression as Ast).renderTo(builder)
        } else {
            prev.renderTo(builder)
        }
        builder.sql(" when ")
        (condValue as Ast).renderTo(builder)
        builder.sql(" then ")
        (value as Ast).renderTo(builder)
    }
}

internal abstract class SimpleCaseExpression<R: Any>(
    private val prev: SimpleMatch<*, R>,
    private val expression: KExpression<R>
) : AbstractKExpression<R>() {

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<R> =
        (prev.startExpression as ExpressionImplementor<R>).type

    override fun accept(visitor: AstVisitor) {
        prev.accept(visitor)
        (expression as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
       usingLowestPrecedence {
           prev.renderTo(builder)
           builder.sql(" else ")
           (expression as Ast).renderTo(builder)
           builder.sql(" end")
       }
    }
}

internal class NonNullSimpleCaseExpression<R: Any>(
    prev: SimpleMatch<*, R>,
    expression: KNonNullExpression<R>
): SimpleCaseExpression<R>(prev, expression), KNonNullExpression<R>

internal class NullableSimpleCaseExpression<R: Any>(
    prev: SimpleMatch<*, R>,
    expression: KExpression<R>
): SimpleCaseExpression<R>(prev, expression), KNullableExpression<R>