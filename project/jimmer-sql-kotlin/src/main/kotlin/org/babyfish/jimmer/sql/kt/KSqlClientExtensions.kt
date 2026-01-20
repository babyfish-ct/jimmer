package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableDelete
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.query.*
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFun
import org.babyfish.jimmer.sql.kt.ast.table.KRecursiveRef
import java.sql.Connection
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

inline fun <reified E : Any, R> KSqlClient.createQuery(
    noinline block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
): KConfigurableRootQuery<KNonNullTable<E>, R> =
    this.createQuery(E::class, block)

inline fun <reified E : Any> KSqlClient.createUpdate(
    noinline block: KMutableUpdate<E>.() -> Unit
): KExecutable<Int> =
    this.createUpdate(E::class, block)

inline fun <reified E : Any> KSqlClient.createDelete(
    noinline block: KMutableDelete<E>.() -> Unit
): KExecutable<Int> =
    this.createDelete(E::class, block)

inline fun <reified E : Any, B : KNonNullBaseTable<*>> KSqlClient.createBaseQuery(
    noinline block: KMutableBaseQuery<E>.() -> KConfigurableBaseQuery<B>
): KConfigurableBaseQuery<B> =
    this.createBaseQuery(E::class, block)

inline fun <reified E : Any, B : KNonNullBaseTable<*>> KSqlClient.createBaseQuery(
    recursiveRef: KRecursiveRef<B>,
    joinBlock: KPropsWeakJoinFun<KNonNullTable<E>, B>,
    noinline block: KMutableRecursiveBaseQuery<E, B>.() -> KConfigurableBaseQuery<B>
): KConfigurableBaseQuery<B> =
    this.createBaseQuery(E::class, recursiveRef, joinBlock, block)

inline fun <reified E : Any, R> KSqlClient.executeQuery(
    limit: Int? = null,
    con: Connection? = null,
    noinline block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
): List<R> =
    this.executeQuery(E::class, limit, con, block)

inline fun <reified E : Any> KSqlClient.executeUpdate(
    con: Connection? = null,
    noinline block: KMutableUpdate<E>.() -> Unit
): Int =
    this.executeUpdate(E::class, con, block)

inline fun <reified E : Any> KSqlClient.executeDelete(
    con: Connection? = null,
    noinline block: KMutableDelete<E>.() -> Unit
): Int =
    this.executeDelete(E::class, con, block)

inline fun <reified T : Any> KSqlClient.findById(id: Any): T? =
    this.findById(T::class, id)

inline fun <reified T : Any> KSqlClient.findByIds(ids: Iterable<*>): List<T> =
    this.findByIds(T::class, ids)

inline fun <K, reified T : Any> KSqlClient.findMapByIds(ids: Iterable<K>): Map<K, T> =
    this.findMapByIds(T::class, ids)

inline fun <reified T : Any> KSqlClient.findOneById(id: Any): T =
    this.findOneById(T::class, id)

inline fun <reified V : View<E>, E : Any> KSqlClient.findAll(
    limit: Int? = null,
    con: Connection? = null,
    noinline block: KMutableRootQuery.ForEntity<E>.() -> Unit = {}
): List<V> =
    this.findAll(V::class, limit, con, block)

inline fun <reified V : View<E>, E : Any> KSqlClient.findOne(
    con: Connection? = null,
    noinline block: KMutableRootQuery.ForEntity<E>.() -> Unit
): V =
    this.findOne(V::class, con, block)

inline fun <reified V : View<E>, E : Any> KSqlClient.findOneOrNull(
    con: Connection? = null,
    noinline block: KMutableRootQuery.ForEntity<E>.() -> Unit
): V? =
    this.findOneOrNull(V::class, con, block)

inline fun <reified E : Any> KSqlClient.deleteById(
    id: Any,
    mode: DeleteMode = DeleteMode.AUTO
): KDeleteResult =
    this.deleteById(E::class, id, mode)

inline fun <reified E : Any> KSqlClient.deleteById(
    id: Any,
    noinline block: KDeleteCommandDsl.() -> Unit
): KDeleteResult =
    this.deleteById(E::class, id, block)

inline fun <reified E : Any> KSqlClient.deleteByIds(
    ids: Iterable<*>,
    mode: DeleteMode = DeleteMode.AUTO
): KDeleteResult =
    this.deleteByIds(E::class, ids, mode)

inline fun <reified E : Any> KSqlClient.deleteByIds(
    ids: Iterable<*>,
    noinline block: KDeleteCommandDsl.() -> Unit
): KDeleteResult =
    this.deleteByIds(E::class, ids, block)