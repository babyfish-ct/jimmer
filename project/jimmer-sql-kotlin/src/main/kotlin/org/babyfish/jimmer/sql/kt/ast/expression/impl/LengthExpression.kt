package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class LengthExpression(
    private var raw: KExpression<String>
) : AbstractKExpression<Int>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(raw)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        raw = ctx.resolveVirtualPredicate(raw)
        return this
    }

    override fun getType(): Class<Int> = Int::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (raw as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sql("length(")
        (raw as Ast).renderTo(builder)
        builder.sql(")")
    }

    class NonNull(
        raw: KExpression<String>
    ) : LengthExpression(raw), KNonNullExpression<Int>

    class Nullable(
        raw: KExpression<String>
    ) : LengthExpression(raw), KNullableExpression<Int>
}