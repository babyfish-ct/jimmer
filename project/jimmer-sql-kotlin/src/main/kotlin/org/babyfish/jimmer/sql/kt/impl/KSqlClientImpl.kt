package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.loader.impl.Loaders
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.cache.CacheDisableConfig
import org.babyfish.jimmer.sql.filter.FilterConfig
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableDeleteImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableUpdateImpl
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.loader.KReferenceLoader
import org.babyfish.jimmer.sql.kt.loader.KValueLoader
import org.babyfish.jimmer.sql.kt.loader.impl.KListLoaderImpl
import org.babyfish.jimmer.sql.kt.loader.impl.KReferenceLoaderImpl
import org.babyfish.jimmer.sql.kt.loader.impl.KValueLoaderImpl
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

    override val caches: KCaches =
        KCachesImpl(sqlClient.caches)

    override val triggers: KTriggers =
        KTriggersImpl(sqlClient.triggers)

    override fun getAssociations(
        prop: KProperty1<*, *>
    ): KAssociations =
        sqlClient.getAssociations(
            prop.toImmutableProp()
        ).let {
            KAssociationsImpl(it)
        }

    override fun <S : Any, V : Any> getValueLoader(prop: KProperty1<S, V>): KValueLoader<S, V> =
        Loaders.createValueLoader<S, V>(
            sqlClient,
            prop.toImmutableProp()
        ).let {
            KValueLoaderImpl(it)
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

    override fun <R> executeNativeSql(master: Boolean, block: (Connection) -> R): R =
        if (master) {
            javaClient.connectionManager
        } else {
            javaClient.getSlaveConnectionManager(false)
        }.execute(block)

    override fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient =
        javaClient
            .caches {
                block(KCacheDisableDsl(it))
            }
            .let {
                if (javaClient === it) {
                    this
                } else {
                    KSqlClientImpl(it)
                }
            }

    override fun filters(block: KFilterDsl.() -> Unit): KSqlClient =
        javaClient
            .filters {
                block(KFilterDsl(it))
            }
            .let {
                if (javaClient === it) {
                    this
                } else {
                    KSqlClientImpl(it)
                }
            }

    override fun disableSlaveConnectionManager(): KSqlClient =
        javaClient.disableSlaveConnectionManager().let {
            if (javaClient === it) {
                this
            } else {
                KSqlClientImpl(it)
            }
        }

    override val javaClient: JSqlClient
        get() = sqlClient
}