package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.Entities
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult
import kotlin.reflect.KClass

fun <E: Any> Entities.findById(entityType: KClass<E>, id: Any): E? =
    findById(entityType.java, id)

fun <E: Any> Entities.findByIds(entityType: KClass<E>, ids: Collection<*>): List<E> =
    findByIds(entityType.java, ids)

fun <ID, E: Any> Entities.findMapByIds(entityType: KClass<E>, ids: Collection<ID>): Map<ID, E> =
    findMapByIds(entityType.java, ids)

inline fun <reified E: Any> Entities.findById(id: Any): E? =
    findById(E::class.java, id)

inline fun <reified E: Any> Entities.findByIds(ids: Collection<*>): List<E> =
    findByIds(E::class.java, ids)

inline fun <reified E: Any, ID> Entities.findMapByIds(ids: Collection<ID>): Map<ID, E> =
    findMapByIds(E::class.java, ids)

fun Entities.delete(entityType: KClass<*>, id: Any): DeleteResult =
    delete(entityType.java, id)

fun Entities.deleteCommand(entityType: KClass<*>, id: Any): DeleteCommand =
    deleteCommand(entityType.java, id)

fun Entities.batchDelete(entityType: KClass<*>, ids: Collection<*>): DeleteResult =
    batchDelete(entityType.java, ids)

fun Entities.batchDeleteCommand(entityType: KClass<*>, ids: Collection<*>): DeleteCommand =
    batchDeleteCommand(entityType.java, ids)
