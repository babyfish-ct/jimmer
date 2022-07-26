package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.fetcher.impl.KFilter
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
    ): Executable<Int>

    fun <E: Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): Executable<Int>

    val queries: KQueries

    val entities: Entities

    fun <S: Any, T: Any> getReferenceAssociation(prop: KProperty1<S, T?>): Associations

    fun <S: Any, T: Any> getListAssociation(prop: KProperty1<S, List<T>>): Associations

    fun <S: Any, T: Any> getReferenceLoader(
        prop: KProperty1<S, T?>,
        filter: KFilter<T>? = null
    ): ReferenceLoader<S, T>

    fun <S: Any, T: Any> getListLoader(
        prop: KProperty1<S, List<T>>,
        filter: KFilter<T>? = null
    ): ListLoader<S, T>

    val javaClient: SqlClient
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val dsl = KSqlClientDsl()
    dsl.block()
    return dsl.buildKSqlClient()
}