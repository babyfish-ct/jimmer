package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.association.loader.Loaders
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableDeleteImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableUpdateImpl
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KSqlClientImpl(
    private val sqlClient: JSqlClient
) : KSqlClient {

    override fun <E : Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int> {
        val update = MutableUpdateImpl(sqlClient, ImmutableType.get(entityType.java))
        block(KMutableUpdateImpl(update))
        update.freeze()
        return KExecutableImpl(update)
    }

    override fun <E : Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int> {
        val delete = MutableDeleteImpl(sqlClient, ImmutableType.get(entityType.java))
        block(KMutableDeleteImpl(delete))
        delete.freeze()
        return KExecutableImpl(delete)
    }

    override val queries: KQueries =
        KQueriesImpl(sqlClient)

    override val entities: KEntities =
        KEntitiesImpl(sqlClient.entities)

    override fun <S: Any, T: Any> getReferenceAssociations(
        prop: KProperty1<S, T?>
    ): KAssociations =
        sqlClient.getAssociations(
            prop.toImmutableProp()
        ).let {
            KAssociationsImpl(it)
        }

    override fun <S: Any, T: Any> getListAssociations(
        prop: KProperty1<S, List<T>>
    ): KAssociations =
        sqlClient.getAssociations(
            prop.toImmutableProp()
        ).let {
            KAssociationsImpl(it)
        }

    override fun <S : Any, T : Any> getReferenceLoader(prop: KProperty1<S, T?>): KReferenceLoader<S, T> =
        Loaders
            .createReferenceLoader<S, T, Table<T>>(
                sqlClient,
                prop.toImmutableProp()
            )
            .let {
                KReferenceLoaderImpl(it)
            }

    override fun <S : Any, T : Any> getListLoader(prop: KProperty1<S, List<T>>): KListLoader<S, T> =
        Loaders
            .createListLoader<S, T, Table<T>>(
                sqlClient,
                prop.toImmutableProp()
            )
            .let {
                KListLoaderImpl(it)
            }

    override fun <R> executeNativeSql(block: (Connection) -> R): R =
        javaClient.connectionManager.execute(block)

    override val javaClient: JSqlClient
        get() = sqlClient
}