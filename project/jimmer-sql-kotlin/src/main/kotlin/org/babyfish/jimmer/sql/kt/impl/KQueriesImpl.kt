package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.association.meta.AssociationType
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.table.AssociationTable
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.KQueries
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableRootQueryImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KQueriesImpl(
    private val sqlClient: JSqlClientImplementor
) : KQueries {

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
    ): KConfigurableRootQuery<KNonNullTable<E>, R> {
        val query = MutableRootQueryImpl<Table<*>>(
            sqlClient,
            ImmutableType.get(entityType.java),
            ExecutionPurpose.QUERY,
            FilterLevel.DEFAULT
        )
        return KMutableRootQueryImpl.ForEntityImpl<E>(
            query as MutableRootQueryImpl<TableLike<*>>
        ).block()
    }

    override fun <S : Any, T : Any, R> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
    ): KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    override fun <S : Any, T : Any, R> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
    ): KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S: Any, T: Any, R> forAssociation(
        immutableProp: ImmutableProp,
        block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
    ): KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R> {
        val associationType = AssociationType.of(immutableProp)
        val query: MutableRootQueryImpl<AssociationTable<S, Table<S>, T, Table<T>>> =
            MutableRootQueryImpl(
                sqlClient,
                associationType,
                ExecutionPurpose.QUERY,
                FilterLevel.DEFAULT
            )
        return KMutableRootQueryImpl.ForAssociation<S, T>(
            query as MutableRootQueryImpl<TableLike<*>>
        ).block()
    }
}