package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal abstract class SubstringExpression(
    private var raw: KExpression<String>,
    private var start: KNonNullExpression<Int>,
    private var length: KNonNullExpression<Int>?
) : AbstractKExpression<String>() {

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(raw) ||
            hasVirtualPredicate(start) ||
            hasVirtualPredicate(length)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        raw = ctx.resolveVirtualPredicate(raw)
        start = ctx.resolveVirtualPredicate(start)
        length = ctx.resolveVirtualPredicate(length)
        return this
    }

    override fun getType(): Class<String> = String::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (raw as Ast).accept(visitor)
        (start as Ast).accept(visitor)
        (length as Ast?)?.accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sqlClient().dialect.renderSubString(
            builder,
            precedence(),
            raw as Ast,
            start as Ast,
            length as Ast?
        )
    }

    class NonNull(
        raw: KExpression<String>,
        start: KNonNullExpression<Int>,
        length: KNonNullExpression<Int>?
    ) : SubstringExpression(raw, start, length), KNonNullExpression<String>

    class Nullable(
        raw: KExpression<String>,
        start: KNonNullExpression<Int>,
        length: KNonNullExpression<Int>?
    ) : SubstringExpression(raw, start, length), KNullableExpression<String>
}