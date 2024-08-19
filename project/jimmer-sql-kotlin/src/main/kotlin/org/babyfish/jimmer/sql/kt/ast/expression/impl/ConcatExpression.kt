package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

internal open class ConcatExpression(
    private var expressions: List<KExpression<String>>
) : AbstractKExpression<String>() {

    init {
        if (expressions.isEmpty()) {
            throw IllegalArgumentException("No expressions")
        }
    }

    override fun getType(): Class<String> = String::class.java

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        for (expression in expressions) {
            (expression as Ast).accept(visitor)
        }
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        builder.sql("concat(")
        var sp = ""
        for (expression in expressions) {
            builder.sql(sp)
            sp = ", "
            (expression as Ast).renderTo(builder)
        }
        builder.sql(")")
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expressions)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        expressions = ctx.resolveVirtualPredicates(expressions)
        return this
    }

    class NonNull(
        expressions: List<KNonNullExpression<String>>
    ) : ConcatExpression(expressions), KNonNullExpression<String>

    class Nullable(
        expressions: List<KExpression<String>>
    ) : ConcatExpression(expressions), KNullableExpression<String>
}