package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableSubQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery

internal abstract class KConfigurableSubQueryImpl<R>(
    internal val javaSubQuery: ConfigurableSubQuery<R>
) : KConfigurableSubQuery<R>,
    Ast by(javaSubQuery as ConfigurableSubQueryImpl<R>),
    ExpressionImplementor<R> by(javaSubQuery as ConfigurableSubQueryImpl<R>) {

    override fun hasVirtualPredicate(): Boolean {
        return (javaSubQuery as Ast).hasVirtualPredicate()
    }

    override fun resolveVirtualPredicate(ctx: AstContext): Ast {
        ctx.resolveVirtualPredicate(javaSubQuery)
        return this
    }

    class NonNull<R: Any>(
        javaSubQuery: ConfigurableSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery),
        KConfigurableSubQuery.NonNull<R> {

        override fun limit(limit: Int): NonNull<R> =
            NonNull(javaSubQuery.limit(limit))

        override fun offset(offset: Long): NonNull<R> =
            NonNull(javaSubQuery.offset(offset))

        override fun limit(limit: Int, offset: Long): NonNull<R> =
            NonNull(javaSubQuery.limit(limit, offset))

        override fun distinct(): NonNull<R> =
            NonNull(javaSubQuery.distinct())

        override fun hint(hint: String?): KConfigurableSubQuery<R> =
            NonNull(javaSubQuery.hint(hint))

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
            KMergedSubQueryImpl.NonNull("union", this, other)

        override fun union(other: KTypedSubQuery.Nullable<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("union", this, other)

        override fun unionAll(other: KTypedSubQuery.NonNull<R>): KTypedSubQuery.NonNull<R> =
            KMergedSubQueryImpl.NonNull("union all", this, other)

        override fun unionAll(other: KTypedSubQuery.Nullable<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("union all", this, other)

        override fun minus(other: KTypedSubQuery<R>): KTypedSubQuery.NonNull<R> =
            KMergedSubQueryImpl.NonNull("minus", this, other)

        override fun intersect(other: KTypedSubQuery<R>): KTypedSubQuery.NonNull<R> =
            KMergedSubQueryImpl.NonNull("intersect", this, other)
    }

    class Nullable<R: Any>(
        javaSubQuery: ConfigurableSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery),
        KConfigurableSubQuery.Nullable<R> {

        override fun limit(limit: Int): Nullable<R> =
            Nullable(javaSubQuery.limit(limit))

        override fun offset(offset: Long): Nullable<R> =
            Nullable(javaSubQuery.offset(offset))

        override fun limit(limit: Int, offset: Long): Nullable<R> =
            Nullable(javaSubQuery.limit(limit, offset))

        override fun distinct(): Nullable<R> =
            Nullable(javaSubQuery.distinct())

        override fun hint(hint: String?): KConfigurableSubQuery<R> =
            Nullable(javaSubQuery.hint(hint))

        override fun union(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("union", this, other)

        override fun unionAll(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("union all", this, other)

        override fun minus(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("minus", this, other)

        override fun intersect(other: KTypedSubQuery<R>): KTypedSubQuery.Nullable<R> =
            KMergedSubQueryImpl.Nullable("intersect", this, other)
    }
}