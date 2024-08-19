package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.impl.table.JoinUtils
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.meta.SingleColumn

internal abstract class NullityPredicate(
    protected val expression: KExpression<*>
) : AbstractKPredicate() {

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) =
        (expression as Ast).accept(visitor)

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        if (expression is PropExpressionImplementor<*>) {
            val partial = expression.getPartial(builder.sqlClient().metadataStrategy)
            if (partial != null) {
                val table = expression.table as TableImplementor<*>
                val prop = expression.prop
                builder.enter(AbstractSqlBuilder.ScopeType.AND)
                for (column in partial) {
                    builder.separator()
                    table.renderSelection(prop, true, builder.assertSimple(),
                        SingleColumn(column, false, null, null)
                    )
                    if (isNegative()) {
                        builder.sql(" is not null")
                    } else {
                        builder.sql(" is null")
                    }
                }
                builder.leave()
            }
        }
        (expression as Ast).renderTo(builder)
        if (isNegative()) {
            builder.sql(" is not null")
        } else {
            builder.sql(" is null")
        }
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expression)

    protected abstract fun isNegative(): Boolean
}

internal class IsNullPredicate(
    expression: KExpression<*>
) : NullityPredicate(expression) {

    init {
        if (expression is PropExpressionImplementor<*>) {
            if (!expression.prop.isNullable && !JoinUtils.hasLeftJoin(expression.table)) {
                throw IllegalArgumentException(
                    "Unable to instantiate `is null` predicate which attempts to check if a " +
                        "non-null property of root table or inner joined table is null " +
                        "(eg: `table.parent.isNull()`). " +
                        "There are two solutions: " +
                        "1. Use associated id property " +
                        "(eg: `table.parentId.isNull()`), " +
                        "2. This non-property must belong to a join table " +
                        "and table join path needs to have at least one left join " +
                        "(eg: `table.`parent?`.isNull()`)." +
                        "The non-null property is `${expression.prop.name}` " +
                            "of table `${expression.table.immutableType.javaClass.name}`."
                )
            }
        }
    }

    override fun isNegative(): Boolean = false

    override fun not(): AbstractKPredicate =
        IsNotNullPredicate(expression)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? =
        IsNullPredicate(ctx.resolveVirtualPredicate(expression))
}

internal class IsNotNullPredicate(
    expression: KExpression<*>
) : NullityPredicate(expression) {

    override fun isNegative(): Boolean = true

    override fun not(): AbstractKPredicate =
        IsNullPredicate(expression)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast =
        IsNotNullPredicate(ctx.resolveVirtualPredicate(expression))
}
