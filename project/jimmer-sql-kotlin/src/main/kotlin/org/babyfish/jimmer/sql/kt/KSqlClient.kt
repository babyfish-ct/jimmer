package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
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

    fun <S: Any, T: Any> getReferenceAssociations(prop: KProperty1<S, T?>): KAssociations

    fun <S: Any, T: Any> getListAssociations(prop: KProperty1<S, List<T>>): KAssociations

    fun <S: Any, T: Any> getReferenceLoader(prop: KProperty1<S, T?>): KReferenceLoader<S, T>

    fun <S: Any, T: Any> getListLoader(prop: KProperty1<S, List<T>>): KListLoader<S, T>

    fun <R> executeNativeSql(block: (Connection) -> R): R

    val javaClient: SqlClient
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = SqlClient.newBuilder()
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}