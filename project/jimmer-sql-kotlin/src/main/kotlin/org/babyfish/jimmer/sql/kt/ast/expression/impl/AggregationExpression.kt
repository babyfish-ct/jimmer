package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class AggregationExpression<T: Any>(
    protected val expression: KExpression<*>
) : AbstractKExpression<T>() {

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(functionName())
        builder.sql("(")
        prefix()?.let {
            builder.sql(it).sql(" ")
        }
        renderChild((expression as Ast), builder)
        builder.sql(")")
    }

    override fun precedence(): Int = 0

    protected abstract fun functionName(): String

    protected open fun prefix(): String? = null

    class Count(
        expression: KExpression<*>
    ): AggregationExpression<Long>(expression), KNonNullExpression<Long> {

        override fun functionName(): String = "count"

        override fun getType(): Class<Long> = Long::class.java
    }

    class CountDistinct(
        expression: KExpression<*>
    ): AggregationExpression<Long>(expression), KNonNullExpression<Long> {

        override fun functionName(): String = "count"

        override fun getType(): Class<Long> = Long::class.java

        override fun prefix(): String? = "distinct"
    }

    class Max<T: Comparable<*>>(
        expression: KExpression<T>
    ): AggregationExpression<T>(expression), KNullableExpression<T> {

        override fun functionName(): String = "max"

        @Suppress("UNCHECKED_CAST")
        override fun getType(): Class<T> =
            (expression as ExpressionImplementor<T>).type
    }

    class Min<T: Comparable<*>>(
        expression: KExpression<T>
    ): AggregationExpression<T>(expression), KNullableExpression<T> {

        override fun functionName(): String = "min"

        @Suppress("UNCHECKED_CAST")
        override fun getType(): Class<T> =
            (expression as ExpressionImplementor<T>).type
    }

    class Sum<T: Number>(
        expression: KExpression<T>
    ): AggregationExpression<T>(expression), KNullableExpression<T> {

        override fun functionName(): String = "sum"

        @Suppress("UNCHECKED_CAST")
        override fun getType(): Class<T> =
            (expression as ExpressionImplementor<T>).type
    }

    class Avg<T: Number>(
        expression: KExpression<T>
    ): AggregationExpression<T>(expression), KNullableExpression<T> {

        override fun functionName(): String = "avg"

        @Suppress("UNCHECKED_CAST")
        override fun getType(): Class<T> =
            (expression as ExpressionImplementor<T>).type
    }
}