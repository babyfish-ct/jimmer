package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.loader.KReferenceLoader
import org.babyfish.jimmer.sql.kt.loader.KValueLoader
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

    fun getAssociations(prop: KProperty1<*, *>): KAssociations

    fun <S: Any, V: Any> getValueLoader(prop: KProperty1<S, V>): KValueLoader<S, V>

    fun <S: Any, T: Any> getReferenceLoader(prop: KProperty1<S, T?>): KReferenceLoader<S, T>

    fun <S: Any, T: Any> getListLoader(prop: KProperty1<S, List<T>>): KListLoader<S, T>

    fun <R> executeNativeSql(master: Boolean = false, block: (Connection) -> R): R

    @NewChain
    fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient

    @NewChain
    fun disableSlaveConnectionManager(): KSqlClient

    val javaClient: JSqlClient
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = JSqlClient.newBuilder()
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}