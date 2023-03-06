package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.SqlExpressions
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import java.lang.IllegalStateException

class SqlDSL internal constructor(
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
        SqlExpressions.parts(sql, expressions, values)
}

internal abstract class AbstractNativeExpression<T: Any>(
    private val type: Class<T>,
    private val parts: List<Any>
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

    override fun renderTo(builder: SqlBuilder) {
        for (part in parts) {
            when (part) {
                is String -> builder.sql(part)
                is KExpression<*> -> renderChild(part as Ast, builder)
                else -> error("Internal bug")
            }
        }
    }
}

internal class NonNullNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNonNullExpression<T>

internal class NullableNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNullableExpression<T>