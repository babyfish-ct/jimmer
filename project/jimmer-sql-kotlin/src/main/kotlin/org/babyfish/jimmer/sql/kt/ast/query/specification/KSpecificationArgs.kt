package org.babyfish.jimmer.sql.kt.ast.query.specification

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.specification.PredicateApplier
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl
import kotlin.reflect.KClass

class KSpecificationArgs<E: Any>(
    val applier: PredicateApplier
) {
    private val query: AbstractMutableStatementImpl = applier.query

    @Suppress("UNCHECKED_CAST")
    private val _table: KNonNullTableEx<E> =
        KNonNullTableExImpl(query.tableLikeImplementor as TableImplementor<E>)

    val table: KNonNullTable<E>
        get() = _table

    fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        query.where(*predicates.map { it?.toJavaPredicate() }.toTypedArray())
    }

    fun <X: Any, R, SQ: KConfigurableSubQuery<R>> subQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<KNonNullTable<E>, KNonNullTableEx<X>>.() -> SQ
    ): SQ =
        subQueries.forEntity(entityType, block)

    fun <X: Any> wildSubQuery(
        entityType: KClass<X>,
        block: KMutableSubQuery<KNonNullTable<E>, KNonNullTableEx<X>>.() -> Unit
    ): KMutableSubQuery<KNonNullTable<E>, KNonNullTableEx<X>> =
        wildSubQueries.forEntity(entityType, block)

    val subQueries: KSubQueries<KNonNullTable<E>> by lazy {
        KSubQueriesImpl(query, _table)
    }

    val wildSubQueries: KWildSubQueries<KNonNullTable<E>> by lazy {
        KWildSubQueriesImpl(query, _table)
    }

    fun <X: Any> child() : KSpecificationArgs<X> =
        KSpecificationArgs(applier)
}