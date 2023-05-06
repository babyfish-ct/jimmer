package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.loader.graphql.impl.LoadersImpl
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
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.kt.loader.impl.KLoadersImpl
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KSqlClientImpl(
    override val javaClient: JSqlClientImplementor
) : KSqlClient {

    // Override it for performance optimization
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> {
        val query = MutableRootQueryImpl<Table<*>>(
            javaClient,
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
        val update = MutableUpdateImpl(javaClient, ImmutableType.get(entityType.java))
        block(KMutableUpdateImpl(update))
        update.freeze()
        return KExecutableImpl(update)
    }

    override fun <E : Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int> {
        val delete = MutableDeleteImpl(javaClient, ImmutableType.get(entityType.java))
        block(KMutableDeleteImpl(delete))
        delete.freeze()
        return KExecutableImpl(delete)
    }

    override val queries: KQueries =
        KQueriesImpl(javaClient)

    override val entities: KEntities =
        KEntitiesImpl(javaClient.entities)

    override val caches: KCaches =
        KCachesImpl(javaClient.caches)

    override val triggers: KTriggers =
        KTriggersImpl(javaClient.triggers)

    override fun getTriggers(transaction: Boolean): KTriggers =
        KTriggersImpl(javaClient.getTriggers(transaction))

    override val filters: KFilters
        get() = KFiltersImpl(javaClient.filters)

    override val loaders: KLoaders =
        KLoadersImpl(javaClient.loaders as LoadersImpl)

    override fun getAssociations(
        prop: KProperty1<*, *>
    ): KAssociations =
        javaClient.getAssociations(
            prop.toImmutableProp()
        ).let {
            KAssociationsImpl(it)
        }

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
        get() = javaClient.entityManager

    override val binLog: BinLog
        get() = javaClient.binLog
}