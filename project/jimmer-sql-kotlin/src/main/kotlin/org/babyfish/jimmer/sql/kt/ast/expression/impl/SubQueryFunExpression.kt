package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class SubQueryFunExpression<T: Any>(
    private var subQuery: KTypedSubQuery<T>
) : AbstractKExpression<T>() {

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> =
        (subQuery as ExpressionImplementor<T>).type

    override fun accept(visitor: AstVisitor) {
        (subQuery as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(funName())
        (subQuery as Ast).renderTo(builder)
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(subQuery)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        subQuery = ctx.resolveVirtualPredicate(subQuery)
        return this
    }

    protected abstract fun funName(): String

    class AllNonNull<T: Any>(
        subQuery: KTypedSubQuery.NonNull<T>
    ) : SubQueryFunExpression<T>(subQuery), KNonNullExpression<T> {

        override fun funName(): String = "all"
    }

    class AllNullable<T: Any>(
        subQuery: KTypedSubQuery.Nullable<T>
    ) : SubQueryFunExpression<T>(subQuery), KNullableExpression<T> {

        override fun funName(): String = "all"
    }

    class AnyNonNull<T: Any>(
        subQuery: KTypedSubQuery.NonNull<T>
    ) : SubQueryFunExpression<T>(subQuery), KNonNullExpression<T> {

        override fun funName(): String = "any"
    }

    class AnyNullable<T: Any>(
        subQuery: KTypedSubQuery.Nullable<T>
    ) : SubQueryFunExpression<T>(subQuery), KNullableExpression<T> {

        override fun funName(): String = "any"
    }
}
