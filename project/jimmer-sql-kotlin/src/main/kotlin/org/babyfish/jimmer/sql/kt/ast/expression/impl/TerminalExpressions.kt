package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KClass

fun <T: Any> value(value: T): KNonNullExpression<T> =
    LiteralExpression(value)

fun <T: Any> nullValue(type: KClass<T>): KNullableExpression<T> =
    NullExpression(type.java)

fun <T: Number> constant(value: T): KNonNullExpression<T> =
    ConstantExpression(value)

internal class LiteralExpression<T: Any>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    override val precedence: Int
        get() = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.variable(value)
    }
}

private class NullExpression<T: Any>(
    private val type: Class<T>
): AbstractKExpression<T>(), KNullableExpression<T> {

    override val precedence: Int
        get() = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.nullVariable(type)
    }
}

private class ConstantExpression<T: Number>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    override val precedence: Int
        get() = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(value.toString())
    }
}