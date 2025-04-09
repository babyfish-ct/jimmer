package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression

object StringFunctions {

    /**
     * Returns a substring starting from the specified position (1-based index).
     */
    @JvmStatic
    fun substring(expression: KExpression<String>, start: Int): KExpression<String> {
        return (expression as ExpressionImplementor<String>).substring(start) as KExpression<String>
    }

    /**
     * Returns a substring starting from the specified position with the specified length.
     */
    @JvmStatic
    fun substring(expression: KExpression<String>, start: Int, length: Int): KExpression<String> {
        return (expression as ExpressionImplementor<String>).substring(start, length) as KExpression<String>
    }

    /**
     * Returns a substring starting from the specified position (1-based index).
     */
    @JvmStatic
    fun substring(expression: KExpression<String>, start: KExpression<Int>): KExpression<String> {
        return (expression as ExpressionImplementor<String>)
            .substring((start as ExpressionImplementor<Int>)) as KExpression<String>
    }

    /**
     * Returns a substring starting from the specified position with the specified length.
     */
    @JvmStatic
    fun substring(
        expression: KExpression<String>, 
        start: KExpression<Int>, 
        length: KExpression<Int>
    ): KExpression<String> {
        return (expression as ExpressionImplementor<String>)
            .substring(
                (start as ExpressionImplementor<Int>),
                (length as ExpressionImplementor<Int>)
            ) as KExpression<String>
    }
} 