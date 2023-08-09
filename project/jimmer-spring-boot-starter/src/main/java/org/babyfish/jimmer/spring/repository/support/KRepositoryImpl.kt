package org.babyfish.jimmer.spring.repository.support

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.repository.*
import org.babyfish.jimmer.sql.ast.mutation.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.fetcher.ViewMetadata
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.springframework.core.GenericTypeResolver
import org.springframework.data.domain.*
import kotlin.reflect.KClass

open class KRepositoryImpl<E: Any, ID: Any> (
    override val sql: KSqlClient,
    entityType: KClass<E>? = null
) : KRepository<E, ID> {

    init {
        Utils.validateSqlClient(sql.javaClient)
    }

    // For bytecode
    protected constructor(sql: KSqlClient, entityType: Class<E>) :
        this(sql, entityType.kotlin)

    @Suppress("UNCHECKED_CAST")
    final override val entityType: KClass<E> =
        if (entityType !== null) {
            entityType
        } else {
            GenericTypeResolver
                .resolveTypeArguments(this.javaClass, JRepository::class.java)
                ?.let { it[0].kotlin as KClass<E> }
                ?: throw IllegalArgumentException(
                    "The class \"" + this.javaClass + "\" " +
                        "does not explicitly specify the type arguments of \"" +
                        JRepository::class.java.name +
                        "\" so that the entityType must be specified"
                )
        }

    override val type: ImmutableType =
        ImmutableType.get(this.entityType.java)

    override fun pager(pageIndex: Int, pageSize: Int): KRepository.Pager {
        return PagerImpl(pageIndex, pageSize)
    }

    override fun pager(pageable: Pageable): KRepository.Pager =
        PagerImpl(pageable.pageNumber, pageable.pageSize)

    override fun findNullable(id: ID, fetcher: Fetcher<E>?): E? =
        if (fetcher !== null) {
            sql.entities.findById(fetcher, id)
        } else {
            sql.entities.findById(entityType, id)
        }

    override fun findByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): List<E> =
        if (fetcher !== null) {
            sql.entities.findByIds(fetcher, Utils.toCollection(ids))
        } else {
            sql.entities.findByIds(entityType, Utils.toCollection(ids))
        }

    override fun findMapByIds(ids: Iterable<ID>, fetcher: Fetcher<E>?): Map<ID, E> =
        if (fetcher !== null) {
            sql.entities.findMapByIds(fetcher, Utils.toCollection(ids))
        } else {
            sql.entities.findMapByIds(entityType, Utils.toCollection(ids))
        }

    override fun findAll(fetcher: Fetcher<E>?): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher)
        } else {
            sql.entities.findAll(entityType)
        }

    override fun findAll(fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher, block)
        } else {
            sql.entities.findAll(entityType, block)
        }

    override fun findAll(fetcher: Fetcher<E>?, sort: Sort): List<E> =
        sql.createQuery(entityType) {
            orderBy(sort)
            select(table.fetch(fetcher))
        }.execute()

    override fun findAll(
        pageIndex: Int,
        pageSize: Int,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): Page<E> =
        sql.createQuery(entityType) {
            orderBy(block)
            select(table.fetch(fetcher))
        }.fetchPage(pageIndex, pageSize)

    override fun findAll(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>?, sort: Sort): Page<E> =
        sql.createQuery(entityType) {
            orderBy(sort)
            select(table.fetch(fetcher))
        }.fetchPage(pageIndex, pageSize)

    override fun findAll(pageable: Pageable): Page<E> =
        findAll(pageable, null)

    override fun findAll(pageable: Pageable, fetcher: Fetcher<E>?): Page<E> =
        sql.createQuery(entityType) {
            orderBy(pageable.sort)
            select(table.fetch(fetcher))
        }.fetchPage(pageable.pageNumber, pageable.pageSize)

    override fun count(): Long =
        sql.createQuery(entityType) {
            select(org.babyfish.jimmer.sql.kt.ast.expression.count(table))
        }.fetchOne()

    override fun <S: E> save(entity: S, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<S> =
        sql.entities.save(entity, block = block)

    override fun <S : E> saveAll(entities: Iterable<S>, block: KSaveCommandDsl.() -> Unit): KBatchSaveResult<S> =
        sql.entities.batchSave(Utils.toCollection(entities), block = block)

    override fun delete(entity: E, mode: DeleteMode): Int =
        sql.entities.delete(
            entityType,
            ImmutableObjects.get(entity, type.idProp)
        ) {
            setMode(mode)
        }.affectedRowCount(entityType)

    override fun deleteById(id: ID, mode: DeleteMode): Int =
        sql.entities.delete(entityType, id) {
            setMode(mode)
        }.affectedRowCount(entityType)

    override fun deleteByIds(ids: Iterable<ID>, mode: DeleteMode): Int =
        sql.entities.batchDelete(entityType, Utils.toCollection(ids)) {
            setMode(mode)
        }.affectedRowCount(entityType)

    override fun deleteAll(entities: Iterable<E>, mode: DeleteMode): Int =
        sql
            .entities
            .batchDelete(
                entityType,
                entities.map {
                    ImmutableObjects.get(it, type.idProp)
                }
            ) {
                setMode(mode)
            }.affectedRowCount(entityType)

    override fun deleteAll() {
        sql.createDelete(entityType) {
            enableDissociation()
        }.execute()
    }

    override fun <V : View<E>> viewer(viewType: KClass<V>): KRepository.Viewer<E, ID, V> =
        ViewerImpl(viewType)

    @Deprecated("Replaced by KConfigurableQuery<E, R>.fetchPage, will be removed in 1.0")
    private class PagerImpl(
        private val pageIndex: Int,
        private val pageSize: Int
    ) : KRepository.Pager {

        override fun <T> execute(query: KConfigurableRootQuery<*, T>): Page<T> =
            query.fetchPage(pageIndex, pageSize)
    }

    private inner class ViewerImpl<V: View<E>>(
        private val viewType: KClass<V>
    ) : KRepository.Viewer<E, ID, V> {

        override fun findNullable(id: ID): V? =
            sql.entities.findById(viewType, id)

        override fun findByIds(ids: Iterable<ID>?): List<V> =
            sql.entities.findByIds(viewType, Utils.toCollection(ids))

        override fun findMapByIds(ids: Iterable<ID>?): Map<ID, V> =
            sql.entities.findMapByIds(viewType, Utils.toCollection(ids))

        override fun findAll(): List<V> =
            sql.entities.findAll(viewType)

        override fun findAll(sort: Sort): List<V> =
            sql.createQuery(entityType) {
                orderBy(sort)
                select(table.fetch(viewType))
            }.execute()

        override fun findAll(block: SortDsl<E>.() -> Unit): List<V> =
            sql.createQuery(entityType) {
                orderBy(block)
                select(table.fetch(viewType))
            }.execute()

        override fun findAll(pageable: Pageable): Page<V> =
            sql.createQuery(entityType) {
                orderBy(pageable.sort)
                select(table.fetch(viewType))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

        override fun findAll(pageIndex: Int, pageSize: Int): Page<V> =
            sql.createQuery(entityType) {
                select(table.fetch(viewType))
            }.fetchPage(pageIndex, pageSize)

        override fun findAll(pageIndex: Int, pageSize: Int, sort: Sort): Page<V> =
            sql.createQuery(entityType) {
                orderBy(sort)
                select(table.fetch(viewType))
            }.fetchPage(pageIndex, pageSize)

        override fun findAll(pageIndex: Int, pageSize: Int, block: SortDsl<E>.() -> Unit): Page<V> =
            sql.createQuery(entityType) {
                orderBy(block)
                select(table.fetch(viewType))
            }.fetchPage(pageIndex, pageSize)
    }
}