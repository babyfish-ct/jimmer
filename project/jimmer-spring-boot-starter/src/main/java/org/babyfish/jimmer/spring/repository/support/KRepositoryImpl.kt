package org.babyfish.jimmer.spring.repository.support

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.repository.*
import org.babyfish.jimmer.sql.ast.mutation.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KConfigurableRootQueryImplementor
import org.springframework.core.GenericTypeResolver
import org.springframework.data.domain.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

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

    override fun findAll(fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)?): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher, block)
        } else {
            sql.entities.findAll(entityType, block)
        }

    override fun findAll(fetcher: Fetcher<E>?, sort: Sort): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher, sort.toSortDslBlock(type))
        } else {
            sql.entities.findAll(entityType, sort.toSortDslBlock(type))
        }

    override fun findAll(
        pageIndex: Int,
        pageSize: Int,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): Page<E> =
        pager(pageIndex, pageSize)
            .execute(
                sql.createQuery(entityType) {
                    orderBy(block)
                    select(table.fetch(fetcher))
                }
            )

    override fun findAll(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>?, sort: Sort): Page<E> =
        pager(pageIndex, pageSize)
            .execute(
                sql.createQuery(entityType) {
                    orderBy(sort)
                    select(table.fetch(fetcher))
                }
            )

    override fun findAll(pageable: Pageable): Page<E> =
        findAll(pageable, null)

    override fun findAll(pageable: Pageable, fetcher: Fetcher<E>?): Page<E> =
        pager(pageable)
            .execute(
                sql.createQuery(entityType) {
                    orderBy(pageable.sort)
                    select(table.fetch(fetcher))
                }
            )

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
        sql.createDelete(entityType) {}.execute()
    }

    private class PagerImpl(
        private val pageIndex: Int,
        private val pageSize: Int
    ) : KRepository.Pager {

        override fun <T> execute(query: KConfigurableRootQuery<*, T>): Page<T> {
            if (pageSize == 0) {
                return PageImpl(query.execute())
            }
            val offset = pageIndex * pageSize
            require(offset <= Int.MAX_VALUE - pageSize) { "offset is too big" }
            val total = query.count()
            val content = query
                .limit(pageSize, offset)
                .execute()
            return PageImpl(
                content,
                PageRequest.of(
                    pageIndex,
                    pageSize,
                    Utils.toSort(
                        (query as KConfigurableRootQueryImplementor<*, *>).javaOrders
                    )
                ),
                total.toLong()
            )
        }
    }
}