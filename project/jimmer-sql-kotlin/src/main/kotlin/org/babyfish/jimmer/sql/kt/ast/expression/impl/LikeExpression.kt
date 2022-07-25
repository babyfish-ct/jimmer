package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class LikePredicate(
    private val negative: Boolean,
    private val expression: KExpression<String>,
    private val insensitive: Boolean,
    private val pattern: String
) : AbstractKPredicate() {

    constructor(
        expression: KExpression<String>,
        insensitive: Boolean,
        pattern: String,
        mode: LikeMode
    ): this(
        false,
        expression,
        insensitive,
        pattern.let {
            var str = it
            if (!mode.isStartExact && !str.startsWith("%")) {
                str = "%$str"
            }
            if (!mode.isEndExact && !str.endsWith("%")) {
                str = "$str%"
            }
            if (insensitive) {
                str = str.lowercase()
            }
            str
        }
    )

    override fun not(): AbstractKPredicate =
        LikePredicate(!negative, expression, insensitive, pattern)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        if (insensitive) {
            builder.sql("lower(")
            (expression as Ast).renderTo(builder)
            builder.sql(")")
        } else {
            (expression as Ast).renderTo(builder)
        }
        builder
            .sql(if (negative) " not like " else " like ")
            .variable(pattern)
    }
}