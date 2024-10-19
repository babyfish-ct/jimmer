package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KExample
import java.sql.Connection
import kotlin.reflect.KClass

/**
 * To be absolutely cache friendly,
 * all query methods in this class that start with "find" ignore the global filters.
 *
 * The mentions here ignore global filters, only for aggregate root objects,
 * excluding deeper objects fetched by object fetcher.
 */
interface KEntities {

    @NewChain
    fun forUpdate() :KEntities

    @NewChain
    fun forConnection(con: Connection?) :KEntities

    fun <T: Any> findById(type: KClass<T>, id: Any): T?

    fun <T : Any> findOneById(type: KClass<T>, id: Any): T

    fun <T: Any> findByIds(type: KClass<T>, ids: Iterable<*>): List<T>

    fun <ID, T: Any> findMapByIds(type: KClass<T>, ids: Iterable<ID>): Map<ID, T>

    fun <E: Any> findById(fetcher: Fetcher<E>, id: Any): E?

    fun <E : Any> findOneById(fetcher: Fetcher<E>, id: Any): E

    fun <E: Any> findByIds(fetcher: Fetcher<E>, ids: Iterable<*>): List<E>

    fun <ID, E: Any> findMapByIds(fetcher: Fetcher<E>, ids: Iterable<ID>): Map<ID, E>

    fun <T: Any> findAll(type: KClass<T>): List<T>

    fun <T: Any> findAll(type: KClass<T>, block: (SortDsl<T>.() -> Unit)? = null): List<T>

    fun <E: Any, V: View<E>> findAllViews(view: KClass<V>, block: (SortDsl<E>.() -> Unit) ?= null): List<V>

    fun <E: Any> findAll(fetcher: Fetcher<E>, block: (SortDsl<E>.() -> Unit)? = null): List<E>

    fun <E: Any> findByExample(
        example: KExample<E>,
        fetcher: Fetcher<E>? = null,
        block: (SortDsl<E>.() -> Unit)? = null
    ): List<E>

    fun <E: Any, V: View<E>> findByExample(
        viewType: KClass<V>,
        example: KExample<E>,
        block: (SortDsl<E>.() -> Unit)? = null
    ): List<V>

    fun <E: Any> save(
        entity: E,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E>

    fun <E: Any> save(
        input: Input<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E>

    fun <E: Any> saveEntities(
        entities: Iterable<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E>

    fun <E: Any> saveInputs(
        entities: Iterable<Input<E>>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E>

    fun delete(
        type: KClass<*>,
        id: Any,
        con: Connection? = null,
        block: (KDeleteCommandDsl.() -> Unit)? = null
    ): KDeleteResult

    fun deleteAll(
        type: KClass<*>,
        ids: Iterable<*>,
        con: Connection? = null,
        block: (KDeleteCommandDsl.() -> Unit)? = null
    ): KDeleteResult
}