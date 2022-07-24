package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class LiteralExpression<T: Any>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> = value::class.java as Class<T>

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.variable(value)
    }
}

internal class NullExpression<T: Any>(
    private val type: Class<T>
): AbstractKExpression<T>(), KNullableExpression<T> {

    override fun getType(): Class<T> = type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.nullVariable(type)
    }
}

internal class ConstantExpression<T: Number>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> = value::class.java as Class<T>

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(value.toString())
    }
}