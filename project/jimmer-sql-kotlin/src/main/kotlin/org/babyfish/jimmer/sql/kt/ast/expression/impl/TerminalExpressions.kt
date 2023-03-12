package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class LiteralExpression<T: Any>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    private var matchedProp: ImmutableProp? = null

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> = value::class.java as Class<T>

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        val scalarProvider = matchedProp?.let {
            builder.astContext.sqlClient.getScalarProvider<Any, Any>(it)
        }
        if (scalarProvider !== null) {
            builder.variable(scalarProvider.toSql(value))
        } else {
            builder.variable(value)
        }
    }

    companion object {

        @JvmStatic
        fun bindPropAndLiteral(mayBeProp: KExpression<*>, mayBeLiteral: KExpression<*>) {
            if (mayBeProp is KPropExpression<*> && mayBeLiteral is LiteralExpression<*>) {
                val matchedProp = (mayBeProp as PropExpressionImplementor<*>).prop
                if (mayBeLiteral.matchedProp !== null && mayBeLiteral.matchedProp !== matchedProp) {
                    throw IllegalStateException(
                        "The matched property of the current literal has already been set, " +
                            "is the current literal expression is shared by difference parts of SQL DSL"
                    )
                }
                mayBeLiteral.matchedProp = matchedProp
            }
        }
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