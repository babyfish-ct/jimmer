package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.association.meta.AssociationType
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.table.AssociationTable
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.KQueries
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableRootQueryImpl
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KQueriesImpl(
    private val sqlClient: JSqlClient
) : KQueries {

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> {
        val query = MutableRootQueryImpl<Table<*>>(
            sqlClient,
            ImmutableType.get(entityType.java),
            false
        )
        val typedQuery = KMutableRootQueryImpl(
            query as MutableRootQueryImpl<Table<E>>
        ).block()
        query.freeze()
        return typedQuery
    }

    override fun <S : Any, T : Any, R> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    override fun <S : Any, T : Any, R> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S: Any, T: Any, R> forAssociation(
        immutableProp: ImmutableProp,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R> {
        val associationType = AssociationType.of(immutableProp)
        val query: MutableRootQueryImpl<AssociationTable<S, Table<S>, T, Table<T>>> =
            MutableRootQueryImpl(
                sqlClient,
                associationType,
                false
            )
        val typedQuery = KMutableRootQueryImpl(
            query as MutableRootQueryImpl<Table<Association<S, T>>>
        ).block()
        query.freeze()
        return typedQuery
    }
}