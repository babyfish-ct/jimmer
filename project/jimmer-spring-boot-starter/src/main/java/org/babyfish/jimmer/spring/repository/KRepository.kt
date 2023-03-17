package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.springframework.core.annotation.AliasFor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@NoRepositoryBean
interface KRepository<E: Any, ID: Any> : PagingAndSortingRepository<E, ID> {

    val sql: KSqlClient

    val type: ImmutableType

    val entityType: KClass<E>

    fun pager(pageIndex: Int, pageSize: Int): Pager

    fun pager(pageable: Pageable): Pager

    fun findNullable(id: ID, fetcher: Fetcher<E>? = null): E?

    override fun findById(id: ID): Optional<E> =
        Optional.ofNullable(findNullable(id))

    fun findById(id: ID, fetcher: Fetcher<E>): Optional<E> =
        Optional.ofNullable(findNullable(id, fetcher))

    @AliasFor("findAllById")
    fun findByIds(ids: Iterable<ID>, fetcher: Fetcher<E>? = null): List<E>

    @AliasFor("findByIds")
    override fun findAllById(ids: Iterable<ID>): List<E> = 
        findByIds(ids)

    fun findMapByIds(ids: Iterable<ID>, fetcher: Fetcher<E>? = null): Map<ID, E>

    override fun findAll(): List<E> =
        findAll(null, null)

    fun findAll(fetcher: Fetcher<E>? = null, block: (SortDsl<E>.() -> Unit)? = null): List<E>

    override fun findAll(sort: Sort): List<E> =
        findAll(null, sort)

    fun findAll(fetcher: Fetcher<E>? = null, sort: Sort): List<E>

    fun findAll(
        pageIndex: Int,
        pageSize: Int,
        fetcher: Fetcher<E>? = null,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Page<E>

    fun findAll(
        pageIndex: Int,
        pageSize: Int,
        fetcher: Fetcher<E>? = null,
        sort: Sort
    ): Page<E>

    override fun findAll(pageable: Pageable): Page<E>

    fun findAll(pageable: Pageable, fetcher: Fetcher<E>? = null): Page<E>

    override fun existsById(id: ID): Boolean =
        findNullable(id) != null
    
    override fun count(): Long

    fun insert(input: Input<E>): E =
        save(input.toEntity(), SaveMode.INSERT_ONLY).modifiedEntity

    fun insert(entity: E): E =
        save(entity, SaveMode.INSERT_ONLY).modifiedEntity

    fun update(input: Input<E>): E =
        save(input.toEntity(), SaveMode.UPDATE_ONLY).modifiedEntity

    fun update(entity: E): E =
        save(entity, SaveMode.UPDATE_ONLY).modifiedEntity

    fun save(input: Input<E>): E =
        save(input.toEntity(), SaveMode.UPSERT).modifiedEntity

    @Suppress("UNCHECKED_CAST")
    override fun <S: E> save(entity: S): S =
        save(entity, SaveMode.UPSERT).modifiedEntity as S

    fun save(input: Input<E>, mode: SaveMode): KSimpleSaveResult<E> =
        save(input.toEntity(), mode)

    fun <S: E> save(entity: S, mode: SaveMode): KSimpleSaveResult<S> =
        save(entity) {
            setAutoAttachingAll()
            setAutoIdOnlyTargetCheckingAll()
            setMode(mode)
        }

    fun save(input: Input<E>, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<E> =
        save(input.toEntity(), block)

    fun <S: E> save(entity: S, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<S>

    override fun <S : E> saveAll(entities: Iterable<S>): List<S> =
        saveAll(entities, SaveMode.UPSERT).simpleResults.map { it.modifiedEntity }

    fun <S : E> saveAll(entities: Iterable<S>, mode: SaveMode): KBatchSaveResult<S> =
        saveAll(entities) {
            setAutoAttachingAll()
            setAutoIdOnlyTargetCheckingAll()
            setMode(mode)
        }

    fun <S : E> saveAll(entities: Iterable<S>, block: KSaveCommandDsl.() -> Unit): KBatchSaveResult<S>

    override fun delete(entity: E) {
        delete(entity, DeleteMode.AUTO)
    }

    fun delete(entity: E, mode: DeleteMode): Int

    override fun deleteById(id: ID) {
        deleteById(id, DeleteMode.AUTO)
    }

    fun deleteById(id: ID, mode: DeleteMode): Int

    @AliasFor("deleteAllById")
    fun deleteByIds(ids: Iterable<ID>) {
        deleteByIds(ids, DeleteMode.AUTO)
    }

    @AliasFor("deleteByIds")
    override fun deleteAllById(ids: Iterable<ID>) {
        deleteByIds(ids, DeleteMode.AUTO)
    }

    fun deleteByIds(ids: Iterable<ID>, mode: DeleteMode): Int

    override fun deleteAll(entities: Iterable<E>) {
        deleteAll(entities, DeleteMode.AUTO)
    }

    fun deleteAll(entities: Iterable<E>, mode: DeleteMode): Int

    override fun deleteAll()

    interface Pager {

        fun <T> execute(query: KConfigurableRootQuery<*, T>): Page<T>
    }
}