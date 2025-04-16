package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class PositionExpression(
    private var subStr: KNonNullExpression<String>,
    private var raw: KExpression<String>,
    private var start: KNonNullExpression<Int>?
) : AbstractKExpression<Int>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(subStr) ||
            hasVirtualPredicate(raw) ||
            hasVirtualPredicate(start)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        subStr = ctx.resolveVirtualPredicate(subStr)
        raw = ctx.resolveVirtualPredicate(raw)
        start = ctx.resolveVirtualPredicate(start)
        return this
    }

    override fun getType(): Class<Int> = Int::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (subStr as Ast).accept(visitor)
        (raw as Ast).accept(visitor)
        (start as Ast?)?.accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sqlClient().dialect.renderPosition(
            builder,
            precedence(),
            subStr as Ast,
            raw as Ast,
            start as Ast?
        )
    }

    class NonNull(
        subStr: KNonNullExpression<String>,
        raw: KExpression<String>,
        start: KNonNullExpression<Int>?
    ) : PositionExpression(subStr, raw, start), KNonNullExpression<Int>

    class Nullable(
        subStr: KNonNullExpression<String>,
        raw: KExpression<String>,
        start: KNonNullExpression<Int>?
    ) : PositionExpression(subStr, raw, start), KNullableExpression<Int>
}