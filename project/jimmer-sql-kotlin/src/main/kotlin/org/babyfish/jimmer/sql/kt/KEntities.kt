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

    fun <E: Any> findById(type: KClass<E>, id: Any): E?

    fun <E: Any> findByIds(type: KClass<E>, ids: Collection<*>): List<E>

    fun <ID, E: Any> findMapByIds(type: KClass<E>, ids: Collection<ID>): Map<ID, E>

    fun <E: Any> findById(fetcher: Fetcher<E>, id: Any): E?

    fun <E: Any> findByIds(fetcher: Fetcher<E>, ids: Collection<*>): List<E>

    fun <ID, E: Any> findMapByIds(fetcher: Fetcher<E>, ids: Collection<ID>): Map<ID, E>

    fun <E: Any> findAll(type: KClass<E>): List<E>

    fun <E: Any> findAll(entityType: KClass<E>, block: (SortDsl<E>.() -> Unit)): List<E>

    fun <E: Any, V: View<E>> findAllViews(view: KClass<V>, block: (SortDsl<E>.() -> Unit)): List<V>

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

    fun <E: Any> saveAll(
        entities: Collection<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E>

    @Deprecated(
        "Will be deleted in 1.0, please use saveAll",
        replaceWith = ReplaceWith("saveAll")
    )
    fun <E: Any> batchSave(
        entities: Collection<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveAll(entities, con, block)

    fun delete(
        type: KClass<*>,
        id: Any,
        con: Connection? = null,
        block: (KDeleteCommandDsl.() -> Unit)? = null
    ): KDeleteResult

    fun deleteAll(
        type: KClass<*>,
        ids: Collection<*>,
        con: Connection? = null,
        block: (KDeleteCommandDsl.() -> Unit)? = null
    ): KDeleteResult

    @Deprecated(
        "Will be deleted in 1.0, please use deleteAll",
        replaceWith = ReplaceWith("deleteAll")
    )
    fun batchDelete(
        type: KClass<*>,
        ids: Collection<*>,
        con: Connection? = null,
        block: (KDeleteCommandDsl.() -> Unit)? = null
    ): KDeleteResult =
        deleteAll(type, ids, con, block)
}