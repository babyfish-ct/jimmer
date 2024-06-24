package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.runtime.SqlBuilder

@Suppress("UNCHECKED_CAST")
internal abstract class KMergedSubQueryImpl<R>(
    private val operator: String,
    private var left: Ast,
    private var right: Ast,
) : KTypedSubQuery<R>, Ast, ExpressionImplementor<R> by (left as ExpressionImplementor<R>) {

    override fun accept(visitor: AstVisitor) {
        left.accept(visitor)
        right.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.enter(AbstractSqlBuilder.ScopeType.SUB_QUERY)
        left.renderTo(builder)
        builder.space('?').sql(operator).space('?')
        right.renderTo(builder)
        builder.leave()
    }

    override fun hasVirtualPredicate(): Boolean =
        left.hasVirtualPredicate() ||
            right.hasVirtualPredicate()

    override fun resolveVirtualPredicate(ctx: AstContext): Ast {
        left = ctx.resolveVirtualPredicate(left)
        right = ctx.resolveVirtualPredicate(right)
        return this
    }

    class NonNull<R: Any>(
        operator: String,
        left: KTypedSubQuery.NonNull<R>,
        right: KTypedSubQuery<R>
    ) : KMergedSubQueryImpl<R>(operator, left as Ast, right as Ast), KTypedSubQuery.NonNull<R> {

        override fun union(other: KTypedSubQuery<R>): KTypedSubQuery<R> =
            if (other is KTypedSubQuery.NonNull<*>) {
                union(other as KTypedSubQuery.NonNull<R>)
            } else {
                union(other as KTypedSubQuery.Nullable<R>)
            }

        override fun unionAll(other: KTypedSubQuery<R>): KTypedSubQuery<R> =
            if (other is KTypedSubQuery.NonNull<*>) {
                unionAll(other as KTypedSubQuery.NonNull<R>)
            } else {
                unionAll(other as KTypedSubQuery.Nullable<R>)
            }

        override fun union(other: KTypedSubQuery.NonNull<R>): KTypedSubQuery.NonNull<R> =
            NonNull("union", this, other)

        override fun union(other: KTypedSubQuery.Nullable<R>): KTypedSubQuery.Nullable<R> =
            Nullable("union", this, other)

        override fun unionAll(other: KTypedSubQuery.NonNull<R>): KTypedSubQuery.NonNull<R> =
            NonNull("union all", this, other)

        override fun unionAll(other: KTypedSubQuery.Nullable<R>): KTypedSubQuery.Nullable<R> =
            Nullable("union all", this, other)

        override fun minus(other: KTypedSubQuery<R>): KTypedSubQuery.NonNull<R> =
            NonNull("minus", this, other)

        override fun intersect(other: KTypedSubQuery<R>): KTypedSubQuery.NonNull<R> =
            NonNull("intersect", this, other)
    }

    class Nullable<R: Any>(
        operator: String,
        left: KTypedSubQuery<R>,
        right: KTypedSubQuery<R>
    ): KMergedSubQueryImpl<R>(operator, left as Ast, right as Ast), KTypedSubQuery.Nullable<R> {

        override fun union(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            Nullable("union", this, other)

        override fun unionAll(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            Nullable("union all", this, other)

        override fun minus(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            Nullable("minus", this, other)

        override fun intersect(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            Nullable("intersect", this, other)
    }
}