package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class NonNullPropExpressionImpl<T: Any>(
    internal val javaPropExpression: PropExpressionImpl<T>
) : AbstractKExpression<T>(), KNonNullPropExpression<T> {

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