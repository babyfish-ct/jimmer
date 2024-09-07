package org.babyfish.jimmer.sql.kt.di

import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.Executor
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

abstract class AbstractKSqlClientDelegate : KSqlClientImplementor {

    protected abstract fun sqlClient(): KSqlClientImplementor

    override val loaders: KLoaders
        get() = sqlClient().loaders

    override fun initialize() {
        sqlClient().initialize()
    }

    override fun <E : Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int> =
        sqlClient().createUpdate(entityType, block)

    override fun <E : Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int> =
        sqlClient().createDelete(entityType, block)

    override val queries: KQueries
        get() = sqlClient().queries

    override val entities: KEntities
        get() = sqlClient().entities

    override val caches: KCaches
        get() = sqlClient().caches

    override val triggers: KTriggers
        get() = sqlClient().triggers

    override fun getTriggers(transaction: Boolean): KTriggers {
        return sqlClient().getTriggers(transaction)
    }

    override val filters: KFilters
        get() = sqlClient().filters

    override fun getAssociations(prop: KProperty1<*, *>): KAssociations {
        return sqlClient().getAssociations(prop)
    }

    override fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient {
        return sqlClient().caches(block)
    }

    override fun filters(block: KFilterDsl.() -> Unit): KSqlClient {
        return sqlClient().filters(block)
    }

    override fun disableSlaveConnectionManager(): KSqlClient {
        return sqlClient().disableSlaveConnectionManager()
    }

    override fun executor(executor: Executor?): KSqlClient {
        return sqlClient().executor(executor)
    }

    override val entityManager: EntityManager
        get() = sqlClient().entityManager

    override val binLog: BinLog
        get() = sqlClient().binLog

    override val javaClient: JSqlClientImplementor
        get() = sqlClient().javaClient
}