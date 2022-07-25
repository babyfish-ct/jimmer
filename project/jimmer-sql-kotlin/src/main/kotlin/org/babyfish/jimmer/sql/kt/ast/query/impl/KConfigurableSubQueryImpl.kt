package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableSubQueryImpl
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery

internal open class KConfigurableSubQueryImpl<R>(
    private val javaSubQuery: TypedSubQuery<R>
) : KConfigurableSubQuery<R>,
    Ast by(javaSubQuery as ConfigurableSubQueryImpl<R>),
    ExpressionImplementor<R> by(javaSubQuery as ConfigurableSubQueryImpl<R>) {

    override fun limit(limit: Int, offset: Int): KConfigurableSubQuery<R> {
        TODO("Not yet implemented")
    }

    override fun distinct(): KConfigurableSubQuery<R> {
        TODO("Not yet implemented")
    }

    class NonNull<R: Any>(
        javaSubQuery: TypedSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery),
        KConfigurableSubQuery.NonNull<R>

    class Nullable<R: Any>(
        javaSubQuery: TypedSubQuery<R>
    ) : KConfigurableSubQueryImpl<R>(javaSubQuery), KConfigurableSubQuery.Nullable<R>
}