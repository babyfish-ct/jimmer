package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.association.loader.Loaders
import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.kt.KQueries
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableDeleteImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableUpdateImpl
import org.babyfish.jimmer.sql.kt.fetcher.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.fetcher.impl.KFilter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

internal class KSqlClientImpl(
    private val sqlClient: SqlClient
) : KSqlClient {

    override fun <E : Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): Executable<Int> {
        val update = MutableUpdateImpl(sqlClient, ImmutableType.get(entityType.java))
        block(KMutableUpdateImpl(update))
        update.freeze()
        return update
    }

    override fun <E : Any> createDelete(entityType: KClass<E>, block: KMutableDelete<E>.() -> Unit): Executable<Int> {
        val delete = MutableDeleteImpl(sqlClient, ImmutableType.get(entityType.java))
        block(KMutableDeleteImpl(delete))
        delete.freeze()
        return delete
    }

    override val queries: KQueries = KQueriesImpl(sqlClient)

    override val entities: Entities
        get() = sqlClient.entities

    override fun <S: Any, T: Any> getReferenceAssociation(prop: KProperty1<S, T?>): Associations =
        sqlClient.getAssociations(
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name)
        )

    override fun <S: Any, T: Any> getListAssociation(prop: KProperty1<S, List<T>>): Associations =
        sqlClient.getAssociations(
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name)
        )

    override fun <S : Any, T : Any> getReferenceLoader(
        prop: KProperty1<S, T?>,
        filter: KFilter<T>?
    ): ReferenceLoader<S, T> =
        Loaders.createReferenceLoader(
            sqlClient,
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name),
            filter?.let {
                FilterWrapper(it)
            }
        )

    override fun <S : Any, T : Any> getListLoader(
        prop: KProperty1<S, List<T>>,
        filter: KFilter<T>?
    ): ListLoader<S, T> =
        Loaders.createListLoader(
            sqlClient,
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name),
            filter?.let {
                FilterWrapper(it)
            }
        )

    override val javaClient: SqlClient
        get() = sqlClient
}