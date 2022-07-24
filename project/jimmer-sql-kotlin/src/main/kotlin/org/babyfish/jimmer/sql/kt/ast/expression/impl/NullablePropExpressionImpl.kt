package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class NullablePropExpressionImpl<T: Any>(
    private val javaPropExpression: PropExpressionImpl<T>
) : AbstractKExpression<T>(), KNullablePropExpression<T> {

    override fun getType(): Class<T> =
        javaPropExpression.type

    override fun precedence(): Int =
        javaPropExpression.precedence()

    override fun accept(visitor: AstVisitor) {
        javaPropExpression.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        javaPropExpression.renderTo(builder)
    }
}