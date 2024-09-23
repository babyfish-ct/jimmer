package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor

internal abstract class AggregationExpression<T: Any>(
    protected var expression: KExpression<*>
) : AbstractKExpression<T>() {

    override fun accept(visitor: AstVisitor) {
        expression.toAst().accept(visitor)
    }

    final override fun renderTo(builder: AbstractSqlBuilder<*>) {

        validate(builder.sqlClient())

        builder.sql(functionName())
        builder.sql("(")
        prefix()?.let {
            builder.sql(it).sql(" ")
        }
        renderExpression(builder)
        builder.sql(")")
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expression)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        ctx.resolveVirtualPredicate(expression)
        return this
    }

    private fun validate(sqlClient: JSqlClientImplementor) {

        if (sqlClient.dialect.isTupleCountSupported) {
            if (expression is KPropExpression<*>) {
                val propExpr = expression as PropExpressionImplementor<*>
                val partial = propExpr.getPartial(sqlClient.metadataStrategy)
                if (partial !== null && partial.size() > 1) {
                    throw IllegalArgumentException(
                        "The `count` function does not support embedded property " +
                            "because multiple columns `count` is not supported by current dialect \"" +
                            sqlClient.getDialect().javaClass.getName() +
                            "\""
                    )
                }
            } else if (expression is TupleExpressionImplementor<*>) {
                throw java.lang.IllegalArgumentException(
                    "The `count` function does not support tuple expression " +
                        "because multiple columns `count` is not supported by current dialect \"" +
                        sqlClient.getDialect().javaClass.getName() +
                        "\""
                )
            }
        }
    }

    protected open fun renderExpression(builder: AbstractSqlBuilder<*>) {
        renderChild((expression as Ast), builder)
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

        override fun prefix(): String = "distinct"

        override fun renderExpression(builder: AbstractSqlBuilder<*>) {
            if (builder.sqlClient().getDialect().isTupleCountSupported) {
                val expression = this.expression
                if (expression is PropExpressionImplementor<*>) {
                    (expression as PropExpressionImplementor<*>).renderTo(builder, true)
                    return
                }
                if (expression is TupleExpressionImplementor<*>) {
                    (expression as TupleExpressionImplementor<*>).renderTo(builder, true)
                    return
                }
            }
            super.renderExpression(builder)
        }
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
    ): AggregationExpression<Double>(expression), KNullableExpression<Double> {

        override fun functionName(): String = "avg"

        override fun getType(): Class<Double> = Double::class.java
    }
}