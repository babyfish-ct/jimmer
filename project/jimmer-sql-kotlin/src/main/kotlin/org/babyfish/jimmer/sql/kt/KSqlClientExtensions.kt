package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <E : Any> KSqlClient.exists(
    type: KClass<E>,
    block: KMutableRootQuery.ForEntity<E>.() -> Unit = {}
): Boolean = queries.forEntity(type) {
    block()
    select(constant(1))
}.limit(1).execute().isNotEmpty()

fun <S : Any, T : Any> KSqlClient.listExists(
    prop: KProperty1<S, List<T>>,
    block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> Unit
): Boolean = queries.forList(prop) {
    block()
    select(constant(1))
}.limit(1).execute().isNotEmpty()

fun <S : Any, T : Any> KSqlClient.refExists(
    prop: KProperty1<S, T>,
    block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> Unit
): Boolean = queries.forReference(prop) {
    block()
    select(constant(1))
}.limit(1).execute().isNotEmpty()