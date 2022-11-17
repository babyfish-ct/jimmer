package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.runtime.EntityManager
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSqlClient {

    fun <E: Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> =
        queries.forEntity(entityType, block)

    fun <E: Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int>

    fun <E: Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int>

    val queries: KQueries

    val entities: KEntities

    val caches: KCaches

    val triggers: KTriggers

    val filters: KFilters

    val loaders: KLoaders

    fun getAssociations(prop: KProperty1<*, *>): KAssociations

    fun <R> executeNativeSql(master: Boolean = false, block: (Connection) -> R): R

    @NewChain
    fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient

    @NewChain
    fun filters(block: KFilterDsl.() -> Unit): KSqlClient

    @NewChain
    fun disableSlaveConnectionManager(): KSqlClient

    val entityManager: EntityManager

    val binLog: BinLog

    val javaClient: JSqlClient
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = JSqlClient.newBuilder()
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}