package org.babyfish.jimmer.spring.repo

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import kotlin.reflect.KClass

/**
 * In earlier versions of Jimmer, type [KotlinRepository]
 * was used to support spring data style repository support.
 *
 * However, based on user feedback, this interface was rarely used. The root causes are:
 * - Unlike JPA and MyBatis, which have lifecycle management objects like EntityManager/Session,
 * Jimmer itself is already designed with a stateless API.
 * Therefore, the stateless abstraction of spring dData style repository is meaningless for Jimmer.</li>
 * - Jimmer itself emphasizes type safety and strives to detect problems at compile-time.
 * spring data's approach based on conventional method names and {@code @Query} annotations
 * would lead to problems only being found at runtime (How Intellij helps certain solutions
 * cheat is not discussed here), which goes against Jimmer's design philosophy.</li>
 *
 * Therefore, developer can simply write a class and annotate it with
 * [org.springframework.data.repository.Repository]. At this point, users can choose to implement this interface or extends class
 * [org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository]. Note, that this is optional, not mandatory.
 */
interface KotlinRepository<E: Any, ID: Any> {

    fun findById(id: ID, fetcher: Fetcher<E>? = null): E?

    fun <V : View<E>> findById(id: ID, viewType: KClass<V>): V?

    fun findByIds(ids: Iterable<ID>): List<E> {
        return findByIds(ids, null as Fetcher<E>?)
    }

    fun findByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): List<E>

    fun <V : View<E>> findByIds(ids: Iterable<ID>, viewType: KClass<V>): List<V>

    fun findMapByIds(ids: Iterable<ID>): Map<ID, E> {
        return findMapByIds(ids, null as Fetcher<E>?)
    }

    fun findMapByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): Map<ID, E>

    fun <V : View<E>> findMapByIds(ids: Iterable<ID>, viewType: KClass<V>): Map<ID, V>

    fun findAll(block: (SortDsl<E>.() -> Unit)? = null): List<E> {
        return findAll(null as Fetcher<E>?, block)
    }

    fun findAll(fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)? = null): List<E>

    fun <V : View<E>> findAll(viewType: KClass<V>, block: (SortDsl<E>.() -> Unit)? = null): List<V>

    fun findPage(pageParam: PageParam, block: (SortDsl<E>.() -> Unit)? = null): Page<E> {
        return findPage(pageParam, null as Fetcher<E>?, block)
    }

    fun findPage(pageParam: PageParam, fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)? = null): Page<E>

    fun <V : View<E>> findPage(
        pageParam: PageParam,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Page<V>

    fun findSlice(limit: Int, offset: Int,block: (SortDsl<E>.() -> Unit)? = null): Slice<E> =
        findSlice(limit, offset, null as Fetcher<E>?, block)

    fun findSlice(
        limit: Int,
        offset: Int,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Slice<E>

    fun <V : View<E>> findSlice(
        limit: Int,
        offset: Int,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Slice<V>

    fun saveCommand(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E>

    fun saveCommand(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> = saveCommand(entity) {
        setMode(mode)
        setAssociatedModeAll(associatedMode)
        block?.invoke(this)
    }

    fun saveCommand(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> =
        saveCommand(input.toEntity(), block)

    fun saveCommand(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> =
        saveCommand(input.toEntity()) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun saveEntitiesCommand(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E>

    fun saveEntitiesCommand(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> = saveEntitiesCommand(entities) {
        setMode(mode)
        setAssociatedModeAll(associatedMode)
        block?.invoke(this)
    }

    fun saveInputsCommand(
        input: Iterable<Input<E>>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> =
        saveEntitiesCommand(input.map { it.toEntity() }, block)

    fun saveInputsCommand(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> =
        saveEntitiesCommand(inputs.map { it.toEntity() }) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun save(
        entity: E ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block)
            .execute(fetcher)

    fun save(
        entity: E ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(fetcher)

    fun saveEntities(
        entities: Iterable<E> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, block)
            .execute(fetcher)

    fun saveEntities(
        entities: Iterable<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(fetcher)

    fun save(
        input: Input<E> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block)
            .execute(fetcher)

    fun save(
        input: Input<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block)
            .execute(fetcher)

    fun saveInputs(
        inputs: Iterable<Input<E>> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, block)
            .execute(fetcher)

    fun saveInputs(
        inputs: Iterable<Input<E>> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(fetcher)

    fun <V: View<E>> save(
        entity: E ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, block)
            .execute(viewType)

    fun <V: View<E>> save(
        entity: E ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(viewType)

    fun <V: View<E>> saveEntities(
        entities: Iterable<E> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, block)
            .execute(viewType)

    fun <V: View<E>> saveEntities(
        entities: Iterable<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(viewType)

    fun <V: View<E>> save(
        input: Input<E> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, block)
            .execute(viewType)

    fun <V: View<E>> save(
        input: Input<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, mode, associatedMode, block)
            .execute(viewType)

    fun <V: View<E>> saveInputs(
        inputs: Iterable<Input<E>> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, block)
            .execute(viewType)

    fun <V: View<E>> saveInputs(
        inputs: Iterable<Input<E>> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(viewType)

    @Deprecated("Please use `save`")
    fun insert(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block !== null) {
                block(this)
            }
        }

    @Deprecated("Please use `save`")
    fun insertIfAbsent(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            if (block !== null) {
                block(this)
            }
        }

    @Deprecated("Please use `save`")
    fun update(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block !== null) {
                block(this)
            }
        }

    @Deprecated("Please use `save`")
    fun merge(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(associatedMode)
            if (block !== null) {
                block(this)
            }
        }

    @Deprecated("Please use `save`", ReplaceWith("insert(input, associatedMode, block)"))
    fun insert(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        insert(input.toEntity(), associatedMode, block)

    @Deprecated("Please use `save`", ReplaceWith("insertIfAbsent(input, associatedMode, block)"))
    fun insertIfAbsent(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        insertIfAbsent(input.toEntity(), associatedMode, block)

    @Deprecated("Please use `save`", ReplaceWith("update(input, associatedMode, block)"))
    fun update(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        update(input.toEntity(), associatedMode, block)

    @Deprecated("Please use `save`", ReplaceWith("merge(input, associatedMode, block)"))
    fun merge(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        merge(input.toEntity(), associatedMode, block)

    fun deleteById(id: ID, deleteMode: DeleteMode = DeleteMode.AUTO): Int

    fun deleteByIds(ids: Iterable<ID>, deleteMode: DeleteMode = DeleteMode.AUTO): Int
}