package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import kotlin.reflect.KClass

interface KSqlClient {

    fun <E: Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> =
        queries.forEntity(entityType, block)

    val queries: KQueries
}

fun newKSqlClient(block: KSqlClientDSL.() -> Unit): KSqlClient {
    val dsl = KSqlClientDSL()
    dsl.block()
    return dsl.buildKSqlClient()
}