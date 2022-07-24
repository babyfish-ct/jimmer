package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import java.lang.IllegalStateException

class SqlDSL internal constructor(
    sql: String
) {
    private val parts = mutableListOf<Any>()

    private var expressionPlaceholder: ExpressionPlaceholder? = null

    private var valuePlaceholder: ValuePlaceholder? = null

    init {
        var startIndex = 0
        while (true) {
            val index = sql.indexOf('%', startIndex)
            if (index == -1) {
                break
            }
            if (index + 1 < parts.size) {
                parts += sql.substring(startIndex, index)
                startIndex = index + when (sql[index + 1]) {
                    'e' -> {
                        addExpressionPlaceholder()
                        2
                    }
                    'v' -> {
                        addValuePlaceholder()
                        2
                    }
                    else -> {
                        1
                    }
                }
            }
            if (startIndex < sql.length) {
                parts += sql.substring(startIndex)
            }
        }
    }

    fun <T: Any> expression(expression: KExpression<*>) {
        val head = expressionPlaceholder
        if (head === null) {
            throw IllegalStateException("Too many expressions")
        }
        head.expression = expression
        expressionPlaceholder = head.next
    }

    fun <T: Any> value(value: T) {
        val head = valuePlaceholder
        if (head === null) {
            throw IllegalStateException("Too many values")
        }
        head.value = value
        valuePlaceholder = head.next
    }

    private fun addExpressionPlaceholder() {
        ExpressionPlaceholder().let {
            parts += it
            val head = expressionPlaceholder
            if (head !== null) {
                head.next = it
            } else {
                expressionPlaceholder = it
            }
        }
    }

    private fun addValuePlaceholder() {
        ValuePlaceholder().let {
            parts += it
            val head = valuePlaceholder
            if (head !== null) {
                head.next = it
            } else {
                valuePlaceholder = it
            }
        }
    }

    fun parts(): List<Any> {
        if (expressionPlaceholder !== null) {
            throw IllegalStateException("Not all the expression placeholders are resolved")
        }
        if (valuePlaceholder !== null) {
            throw IllegalStateException("Not all the value placeholders are resolved")
        }
        return parts
    }
}

private class ExpressionPlaceholder {
    var expression: KExpression<*>? = null
    var next: ExpressionPlaceholder? = null
}

private class ValuePlaceholder {
    var value: Any? = null
    var next: ValuePlaceholder? = null
}

internal abstract class AbstractNativeExpression<T: Any>(
    private val type: Class<T>,
    private val parts: List<Any>
) : AbstractKExpression<T>() {

    override fun getType(): Class<T> = type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        for (part in parts) {
            if (part is ExpressionPlaceholder) {
                (part.expression as Ast).accept(visitor)
            }
        }
    }

    override fun renderTo(builder: SqlBuilder) {
        for (part in parts) {
            when (part) {
                is String -> builder.sql(part)
                is ExpressionPlaceholder -> renderChild(part.expression as Ast, builder)
                is ValuePlaceholder -> builder.variable(part.value)
                else -> error("Internal bug")
            }
        }
    }
}

internal class NonNullNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNonNullExpression<T>

internal class NullableNativeExpression<T: Any>(type: Class<T>, parts: List<Any>) :
    AbstractNativeExpression<T>(type, parts), KNullableExpression<T>