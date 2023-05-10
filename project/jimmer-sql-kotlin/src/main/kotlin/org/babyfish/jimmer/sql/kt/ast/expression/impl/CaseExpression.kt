package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KClass

object CaseStarter {
    fun <R: Any> match(
        cond: KNonNullExpression<Boolean>,
        value: KNonNullExpression<R>
    ): NonNullCase<R> =
        NonNullCase(Match(null, cond, value))

    fun <R: Any> match(
        cond: KNonNullExpression<Boolean>,
        value: R
    ): NonNullCase<R> =
        NonNullCase(Match(null, cond, value(value)))

    fun <R: Any> match(
        cond: KNonNullExpression<Boolean>,
        value: KNullableExpression<R>
    ): NullableCase<R> =
        NullableCase(Match(null, cond, value))
}

class NonNullCase<R: Any> internal constructor(
    private val match: Match<R>
) {
    fun match(
        cond: KNonNullExpression<Boolean>,
        value: KNonNullExpression<R>
    ): NonNullCase<R> =
        NonNullCase(Match(match, cond, value))

    fun match(
        cond: KNonNullExpression<Boolean>,
        value: R
    ): NonNullCase<R> =
        NonNullCase(Match(match, cond, value(value)))

    fun match(
        cond: KNonNullExpression<Boolean>,
        value: KNullableExpression<R>
    ): NullableCase<R> =
        NullableCase(Match(match, cond, value))

    fun otherwise(value: KNonNullExpression<R>): KNonNullExpression<R> =
        NonNullCaseExpression(match, value)

    fun otherwise(value: R): KNonNullExpression<R> =
        NonNullCaseExpression(match, value(value))

    fun otherwise(value: KNullableExpression<R>): KNullableExpression<R> =
        NullableCaseExpression(match, value)

    @Suppress("UNCHECKED_CAST")
    fun otherwise(): KNullableExpression<R> =
        NullableCaseExpression(
            match,
            nullValue((match.value as AbstractKExpression<*>).type.kotlin as KClass<R>)
        )
}

class NullableCase<R: Any> internal constructor(
    private val match: Match<R>
) {
    fun match(
        cond: KNonNullExpression<Boolean>,
        value: KExpression<R>
    ): NullableCase<R> =
        NullableCase(Match(match, cond, value))

    fun match(
        cond: KNonNullExpression<Boolean>,
        value: R
    ): NullableCase<R> =
        NullableCase(Match(match, cond, value(value)))

    fun otherwise(value: KExpression<R>): KNullableExpression<R> =
        NullableCaseExpression(match, value)

    @Suppress("UNCHECKED_CAST")
    fun otherwise(value: R? = null): KNullableExpression<R> =
        if (value !== null) {
            NullableCaseExpression(match, value(value))
        } else {
            NullableCaseExpression(
                match,
                nullValue((match.value as AbstractKExpression<*>).type.kotlin as KClass<R>)
            )
        }
}

internal data class Match<R: Any>(
    val prev: Match<R>?,
    val cond: KExpression<Boolean>,
    val value: KExpression<R>
) {
    fun accept(visitor: AstVisitor) {
        prev?.accept(visitor)
        (cond as Ast).accept(visitor)
        (value as Ast).accept(visitor)
    }

    fun renderTo(builder: SqlBuilder) {
        val prev = this.prev
        if (prev === null) {
            builder.sql("case")
        } else {
            prev.renderTo(builder)
        }
        builder.sql(" when ")
        (cond as Ast).renderTo(builder)
        builder.sql(" then ")
        (value as Ast).renderTo(builder)
    }
}

internal abstract class CaseExpression<R: Any>(
    private val prev: Match<R>,
    private val expression: KExpression<R>
) : AbstractKExpression<R>() {

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<R> =
        (expression as ExpressionImplementor<R>).type

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

internal class NonNullCaseExpression<R: Any>(
    prev: Match<R>,
    expression: KNonNullExpression<R>
): CaseExpression<R>(prev, expression), KNonNullExpression<R>

internal class NullableCaseExpression<R: Any>(
    prev: Match<R>,
    expression: KExpression<R>
): CaseExpression<R>(prev, expression), KNullableExpression<R>