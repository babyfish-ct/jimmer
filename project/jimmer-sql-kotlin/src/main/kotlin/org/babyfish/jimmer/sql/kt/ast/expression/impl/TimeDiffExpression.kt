package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.SqlTimeUnit
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal open class TimeDiffExpression<T: Any>(
    private var raw: KExpression<T>,
    private var other: KExpression<T>,
    private val timeUnit: SqlTimeUnit
) : AbstractKExpression<Float>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(raw) ||
            hasVirtualPredicate(other)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        raw = ctx.resolveVirtualPredicate(raw)
        other = ctx.resolveVirtualPredicate(other)
        return this
    }

    override fun getType(): Class<Float> =
        Float::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (raw as Ast).accept(visitor)
        (other as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sqlClient().dialect.renderTimeDiff(
            builder,
            precedence(),
            raw as Ast,
            other as Ast,
            timeUnit
        )
    }

    class NonNull<T: Any>(
        raw: KExpression<T>,
        other: KExpression<T>,
        timeUnit: SqlTimeUnit
    ) : TimeDiffExpression<T>(raw, other, timeUnit), KNonNullExpression<Float>

    class Nullable<T: Any>(
        raw: KExpression<T>,
        other: KExpression<T>,
        timeUnit: SqlTimeUnit
    ) : TimeDiffExpression<T>(raw, other, timeUnit), KNullableExpression<Float>
}