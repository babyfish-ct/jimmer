package org.babyfish.jimmer.sql.kt.di

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.exception.DatabaseValidationException
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.*
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.Executor
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import org.babyfish.jimmer.sql.transaction.Propagation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

abstract class AbstractKSqlClientDelegate : KSqlClientImplementor {

    protected abstract fun sqlClient(): KSqlClientImplementor

    override val loaders: KLoaders
        get() = sqlClient().loaders

    override fun <B : KNonNullBaseTable<*>, R> createQuery(
        symbol: KBaseTableSymbol<B>,
        block: KMutableRootQuery<B>.() -> KConfigurableRootQuery<B, R>
    ): KConfigurableRootQuery<B, R> =
        sqlClient().createQuery(symbol, block)

    override fun <E : Any, B : KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        block: KMutableBaseQuery<E>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B> =
        sqlClient().createBaseQuery(entityType, block)

    override fun <E : Any, B : KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        recursiveRef: KRecursiveRef<B>,
        joinBlock: KPropsWeakJoinFun<KNonNullTable<E>, B>,
        block: KMutableRecursiveBaseQuery<E, B>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B> =
        sqlClient().createBaseQuery(entityType, recursiveRef, joinBlock, block)

    override fun <E : Any, B : KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        recursiveRef: KRecursiveRef<B>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, B>>,
        block: KMutableRecursiveBaseQuery<E, B>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B> =
        sqlClient().createBaseQuery(entityType, recursiveRef, weakJoinType, block)

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

    override fun <R> transaction(propagation: Propagation, block: () -> R): R =
        sqlClient().transaction(propagation, block)

    override fun validateDatabase(): DatabaseValidationException? =
        sqlClient().validateDatabase()

    override val javaClient: JSqlClientImplementor
        get() = sqlClient().javaClient
}