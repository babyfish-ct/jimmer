package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.table.KWeakJoinImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

abstract class KWeakJoin<S: Any, T: Any> : WeakJoin<Table<S>, Table<T>>,
    KWeakJoinImplementor<S, T> {

    final override fun on(
        source: TableLike<S>,
        target: TableLike<T>,
        statement: AbstractMutableStatementImpl
    ): Predicate? {
        val st = KNonNullTableExImpl(source as TableImplementor<S>, JOIN_ERROR_REASON)
        val tt = KNonNullTableExImpl(target as TableImplementor<T>, JOIN_ERROR_REASON)
        return on(
            st,
            tt,
            ContextImpl(statement, st, tt)
        )?.toJavaPredicate()
    }

    open fun on(
        source: KNonNullTable<S>,
        target: KNonNullTable<T>
    ): KNonNullExpression<Boolean>? = null

    open fun on(
        source: KNonNullTable<S>,
        target: KNonNullTable<T>,
        ctx: Context<S, T>
    ): KNonNullExpression<Boolean>? =
        on(source, target)

    interface Context<S: Any, T: Any> {
        val sourceSubQueries: KSubQueries<KNonNullTableEx<S>>
        val sourceWildSubQueries: KWildSubQueries<KNonNullTableEx<S>>
        val targetSubQueries: KSubQueries<KNonNullTableEx<T>>
        val targetWildSubQueries: KWildSubQueries<KNonNullTableEx<T>>
    }

    final override fun on(source: Table<S>, target: Table<T>): Predicate {
        throw UnsupportedOperationException(
            "The method with 2 arguments is forbidden"
        )
    }

    private class ContextImpl<S: Any, T: Any>(
        statement: AbstractMutableStatementImpl,
        source: KNonNullTableEx<S>,
        target: KNonNullTableEx<T>
    ) : Context<S, T> {
        override val sourceSubQueries: KSubQueries<KNonNullTableEx<S>> =
            KSubQueriesImpl(statement, source)
        override val sourceWildSubQueries: KWildSubQueries<KNonNullTableEx<S>> =
            KWildSubQueriesImpl(statement, source)
        override val targetSubQueries: KSubQueries<KNonNullTableEx<T>> =
            KSubQueriesImpl(statement, target)
        override val targetWildSubQueries: KWildSubQueries<KNonNullTableEx<T>> =
            KWildSubQueriesImpl(statement, target)
    }

    companion object {
        private val JOIN_ERROR_REASON = "it is forbidden in the implementation of \"" +
            KWeakJoin::class.java.name +
            "\""
    }
}