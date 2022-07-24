package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.SqlClient
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.KQueries
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableRootQueryImpl
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KQueriesImpl(
    private val sqlClient: SqlClient
) : KQueries {

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> {
        val query = MutableRootQueryImpl<Table<*>>(
            sqlClient,
            ImmutableType.get(entityType.java)
        )
        val typedQuery = KMutableRootQueryImpl(
            query as MutableRootQuery<Table<E>>,
            query.table as TableImplementor<E>
        ).block()
        query.freeze()
        return typedQuery
    }

    override fun <S : Any, T : Any, R> forReference(
        prop: KProperty1<S, R?>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R> {
        TODO("Not yet implemented")
    }

    override fun <S : Any, T : Any, R> forList(
        prop: KProperty1<S, List<R>>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R> {
        TODO("Not yet implemented")
    }
}