package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.SqlTimeUnit
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal open class TimePlusExpression<T: Any>(
    private var raw: KExpression<T>,
    private var value: KExpression<Long>,
    private val timeUnit: SqlTimeUnit
) : AbstractKExpression<T>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(raw) ||
            hasVirtualPredicate(value)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        raw = ctx.resolveVirtualPredicate(raw)
        value = ctx.resolveVirtualPredicate(value)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> =
        (raw as ExpressionImplementor<T>).type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (raw as Ast).accept(visitor)
        (value as Ast).accept(visitor);
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sqlClient().dialect.renderTimePlus(
            builder,
            precedence(),
            raw as Ast,
            value as Ast,
            timeUnit
        )
    }

    class NonNull<T: Any>(
        raw: KExpression<T>,
        value: KExpression<Long>,
        timeUnit: SqlTimeUnit
    ) : TimePlusExpression<T>(raw, value, timeUnit), KNonNullExpression<T>

    class Nullable<T: Any>(
        raw: KExpression<T>,
        value: KExpression<Long>,
        timeUnit: SqlTimeUnit
    ) : TimePlusExpression<T>(raw, value, timeUnit), KNullableExpression<T>
}