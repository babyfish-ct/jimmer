package org.babyfish.jimmer.spring.repo.support

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.spring.repo.KotlinRepository
import org.babyfish.jimmer.spring.repo.PageParam
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.DtoMetadata
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandPartialDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.springframework.core.GenericTypeResolver
import kotlin.reflect.KClass

/**
 * The base implementation of [KotlinRepository]
 *
 * If the repoistory
 */
abstract class AbstractKotlinRepository<E: Any, ID: Any>(
    protected val sql: KSqlClient
) : KotlinRepository<E, ID> {

    @Suppress("UNCHECKED_CAST")
    protected val entityType: KClass<E> =
        GenericTypeResolver
            .resolveTypeArguments(this.javaClass, KotlinRepository::class.java)
            ?.let { it[0].kotlin as KClass<E> }
            ?: throw IllegalArgumentException(
                "The class \"" + this.javaClass + "\" " +
                    "does not explicitly specify the type arguments of \"" +
                    KotlinRepository::class.java.name +
                    "\" so that the entityType must be specified"
            )

    protected val immutableType: ImmutableType =
        ImmutableType.get(this.entityType.java)

    override fun findById(id: ID, fetcher: Fetcher<E>?): E? =
        if (fetcher == null) {
            sql.findById(entityType, id)
        } else {
            sql.findById(fetcher, id)
        }

    override fun <V : View<E>> findById(id: ID, viewType: KClass<V>): V? =
        sql.findById(viewType, id)

    override fun findByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): List<E> =
        if (fetcher == null) {
            sql.findByIds(entityType, ids)
        } else {
            sql.findByIds(fetcher, ids)
        }

    override fun <V : View<E>> findByIds(ids: Iterable<ID>, viewType: KClass<V>): List<V> =
        sql.findByIds(viewType, ids)

    override fun findMapByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): Map<ID, E> =
        if (fetcher == null) {
            sql.findMapByIds(entityType, ids)
        } else {
            sql.findMapByIds(fetcher, ids)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <V : View<E>> findMapByIds(ids: Iterable<ID>, viewType: KClass<V>): Map<ID, V> =
        DtoMetadata.of(viewType.java).let { metadata ->
            val idPropId = immutableType.idProp.id
            sql.findByIds(metadata.fetcher, ids).associateBy({
                (it as ImmutableSpi).__get(idPropId) as ID
            }) {
                metadata.converter.apply(it)
            }
        }

    override fun findAll(fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)?): List<E> =
        if (fetcher == null) {
            sql.entities.findAll(entityType, block)
        } else {
            sql.entities.findAll(fetcher, block)
        }

    override fun <V : View<E>> findAll(viewType: KClass<V>, block: (SortDsl<E>.() -> Unit)?): List<V> =
        sql.entities.findAllViews(viewType, block)

    override fun findPage(
        pageParam: PageParam,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): Page<E> =
        sql.createQuery(entityType) {
            orderBy(block)
            select(table.fetch(fetcher))
        }.fetchPage(pageParam.index, pageParam.size)

    override fun <V : View<E>> findPage(
        pageParam: PageParam,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)?
    ): Page<V> =
        sql.createQuery(entityType) {
            orderBy(block)
            select(table.fetch(viewType))
        }.fetchPage(pageParam.index, pageParam.size)

    override fun findSlice(
        limit: Int,
        offset: Int,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): Slice<E> =
        sql.createQuery(entityType) {
            orderBy(block)
            select(table.fetch(fetcher))
        }.fetchSlice(limit, offset)

    override fun <V : View<E>> findSlice(
        limit: Int,
        offset: Int,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)?
    ): Slice<V> =
        sql.createQuery(entityType) {
            orderBy(block)
            select(table.fetch(viewType))
        }.fetchSlice(limit, offset)

    override fun save(entity: E, block: (KSaveCommandDsl.() -> Unit)?): KSimpleSaveResult<E> =
        sql.entities.save(entity, null, block)

    override fun save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        sql.entities.save(entity, null) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        sql.entities.save(entity, null) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun saveEntities(entities: Iterable<E>, block: (KSaveCommandDsl.() -> Unit)?): KBatchSaveResult<E> =
        sql.entities.saveEntities(entities, null, block)

    override fun saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> =
        sql.entities.saveEntities(entities) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> =
        sql.entities.saveEntities(entities) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun save(input: Input<E>, block: (KSaveCommandDsl.() -> Unit)?): KSimpleSaveResult<E> =
        sql.entities.save(input, null, block)

    override fun save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        sql.entities.save(input) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        sql.entities.save(input) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun saveInputs(inputs: Iterable<Input<E>>, block: (KSaveCommandDsl.() -> Unit)?): KBatchSaveResult<E> =
        sql.entities.saveInputs(inputs, null, block)

    override fun saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> =
        sql.entities.saveInputs(inputs) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> =
        sql.entities.saveInputs(inputs) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    override fun deleteById(id: ID, deleteMode: DeleteMode): Int =
        sql.deleteById(entityType, id, deleteMode).affectedRowCount(entityType)

    override fun deleteByIds(ids: Iterable<ID>, deleteMode: DeleteMode): Int =
        sql.deleteByIds(entityType, ids, deleteMode).affectedRowCount(entityType)
}