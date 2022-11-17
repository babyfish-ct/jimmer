package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.loader.impl.LoadersImpl
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableDeleteImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KMutableUpdateImpl
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableRootQueryImpl
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.filter.impl.KFiltersImpl
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.kt.loader.KReferenceLoader
import org.babyfish.jimmer.sql.kt.loader.KValueLoader
import org.babyfish.jimmer.sql.kt.loader.impl.KListLoaderImpl
import org.babyfish.jimmer.sql.kt.loader.impl.KLoadersImpl
import org.babyfish.jimmer.sql.kt.loader.impl.KReferenceLoaderImpl
import org.babyfish.jimmer.sql.kt.loader.impl.KValueLoaderImpl
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KSqlClientImpl(
    private val sqlClient: JSqlClient
) : KSqlClient {

    // Override it for performance optimization
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> {
        val query = MutableRootQueryImpl<Table<*>>(
            sqlClient,
            ImmutableType.get(entityType.java),
            ExecutionPurpose.QUERY,
            false
        )
        return KMutableRootQueryImpl(
            query as MutableRootQueryImpl<Table<E>>
        ).block()
    }

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

    override val filters: KFilters
        get() = KFiltersImpl(javaClient.filters)

    override val loaders: KLoaders =
        KLoadersImpl(javaClient.loaders as LoadersImpl)

    override fun getAssociations(
        prop: KProperty1<*, *>
    ): KAssociations =
        sqlClient.getAssociations(
            prop.toImmutableProp()
        ).let {
            KAssociationsImpl(it)
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

    override val entityManager: EntityManager
        get() = sqlClient.entityManager

    override val binLog: BinLog
        get() = sqlClient.binLog

    override val javaClient: JSqlClient
        get() = sqlClient
}