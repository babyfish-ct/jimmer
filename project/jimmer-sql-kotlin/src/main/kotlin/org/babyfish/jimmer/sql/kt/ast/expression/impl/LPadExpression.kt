package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class LPadExpression(
    private var raw: KExpression<String>,
    private var length: KNonNullExpression<Int>,
    private var pad: KNonNullExpression<String>
) : AbstractKExpression<String>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(raw) ||
            hasVirtualPredicate(length) ||
            hasVirtualPredicate(pad)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        raw = ctx.resolveVirtualPredicate(raw)
        length = ctx.resolveVirtualPredicate(length)
        pad = ctx.resolveVirtualPredicate(pad)
        return this
    }

    override fun getType(): Class<String> = String::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (raw as Ast).accept(visitor)
        (length as Ast).accept(visitor)
        (pad as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sqlClient().dialect.renderLPad(
            builder,
            precedence(),
            raw as Ast,
            length as Ast,
            pad as Ast
        )
    }

    class NonNull(
        raw: KExpression<String>,
        length: KNonNullExpression<Int>,
        pad: KNonNullExpression<String>,
    ) : LPadExpression(raw, length, pad), KNonNullExpression<String>

    class Nullable(
        raw: KExpression<String>,
        length: KNonNullExpression<Int>,
        pad: KNonNullExpression<String>,
    ) : LPadExpression(raw, length, pad), KNullableExpression<String>
}