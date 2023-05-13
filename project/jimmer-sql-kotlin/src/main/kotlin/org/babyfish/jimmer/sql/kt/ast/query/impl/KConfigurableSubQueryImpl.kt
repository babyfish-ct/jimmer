package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableSubQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery

internal open abstract class KConfigurableSubQueryImpl<R>(
    protected val javaSubQuery: ConfigurableSubQuery<R>
) : KConfigurableSubQuery<R>,
    Ast by(javaSubQuery as ConfigurableSubQueryImpl<R>),
    ExpressionImplementor<R> by(javaSubQuery as ConfigurableSubQueryImpl<R>) {

    class NonNull<R: Any>(
        javaSubQuery: ConfigurableSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery),
        KConfigurableSubQuery.NonNull<R> {

        override fun limit(limit: Int, offset: Int): KConfigurableSubQuery<R> =
            NonNull(javaSubQuery.limit(limit, offset))

        override fun distinct(): KConfigurableSubQuery<R> =
            NonNull(javaSubQuery.distinct())

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

        override fun limit(limit: Int, offset: Int): KConfigurableSubQuery<R> =
            Nullable(javaSubQuery.limit(limit, offset))

        override fun distinct(): KConfigurableSubQuery<R> =
            Nullable(javaSubQuery.distinct())

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