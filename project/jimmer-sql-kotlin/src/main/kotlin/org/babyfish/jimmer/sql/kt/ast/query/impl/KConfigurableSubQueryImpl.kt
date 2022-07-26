package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableSubQueryImpl
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery

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
    }

    class Nullable<R: Any>(
        javaSubQuery: ConfigurableSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery),
        KConfigurableSubQuery.Nullable<R> {

        override fun limit(limit: Int, offset: Int): KConfigurableSubQuery<R> =
            Nullable(javaSubQuery.limit(limit, offset))

        override fun distinct(): KConfigurableSubQuery<R> =
            Nullable(javaSubQuery.distinct())
    }
}