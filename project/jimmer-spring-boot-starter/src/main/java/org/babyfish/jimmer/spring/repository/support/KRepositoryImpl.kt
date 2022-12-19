package org.babyfish.jimmer.spring.repository.support

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.model.Input
import org.babyfish.jimmer.spring.repository.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.FindDsl
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
    protected val entityType: KClass<E> =
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

    protected val immutableType: ImmutableType =
        ImmutableType.get(this.entityType.java)

    override fun pager(pageIndex: Int, pageSize: Int, block: (FindDsl<E>.() -> Unit)?): KRepository.Pager<E> {
        val sort = block?.toSort() ?: Sort.unsorted()
        return PagerImpl(PageRequest.of(pageIndex, pageSize, sort))
    }

    override fun pager(pageable: Pageable): KRepository.Pager<E> =
        PagerImpl(pageable)

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

    override fun findAll(fetcher: Fetcher<E>?, block: (FindDsl<E>.() -> Unit)?): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher, block)
        } else {
            sql.entities.findAll(entityType, block)
        }

    override fun findAll(fetcher: Fetcher<E>?, sort: Sort): List<E> =
        if (fetcher !== null) {
            sql.entities.findAll(fetcher, sort.toFindDslBlock(immutableType))
        } else {
            sql.entities.findAll(entityType, sort.toFindDslBlock(immutableType))
        }

    override fun findAll(
        pageIndex: Int,
        pageSize: Int,
        fetcher: Fetcher<E>?,
        block: (FindDsl<E>.() -> Unit)?
    ): Page<E> =
        pager(pageIndex, pageSize, block)
            .execute(
                sql.createQuery(entityType) {
                    orderBy(block)
                    select(table.fetch(fetcher))
                }
            )

    override fun findAll(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>?, sort: Sort): Page<E> =
        pager(pageIndex, pageSize, sort)
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

    override fun <S : E> save(entity: S): S =
        sql.entities.save(entity) {
            setAutoAttachingAll()
        }.modifiedEntity

    override fun <S : E> saveAll(entities: Iterable<S>): List<S> =
        sql.entities.batchSave(Utils.toCollection(entities)) {
            setAutoAttachingAll()
        }.simpleResults.map { it.modifiedEntity }

    override fun save(input: Input<E>): E =
        sql.entities.save(input.toEntity()) {
            setAutoAttachingAll()
        }.modifiedEntity

    override fun delete(entity: E) {
        sql.entities.delete(entityType, ImmutableObjects.get(entity, immutableType.idProp))
    }

    override fun deleteById(id: ID) {
        sql.entities.delete(entityType, id)
    }

    override fun deleteByIds(ids: Iterable<ID>) {
        sql.entities.batchDelete(entityType, Utils.toCollection(ids))
    }

    override fun deleteAll() {
        sql.createDelete(entityType) {}.execute()
    }

    override fun deleteAll(entities: Iterable<E>) {
        sql
            .entities
            .batchDelete(
                entityType,
                entities.map {
                    ImmutableObjects.get(it, immutableType.idProp)
                }
            )
    }

    private class PagerImpl<E>(
        private val pageable: Pageable
    ) : KRepository.Pager<E> {

        override fun execute(query: KConfigurableRootQuery<*, E>): Page<E> {
            if (pageable.pageSize == 0) {
                return PageImpl(query.execute())
            }
            val offset = pageable.offset
            require(offset <= Int.MAX_VALUE - pageable.pageSize) { "offset is too big" }
            val total = query.count()
            val content = query
                .limit(pageable.pageSize, offset.toInt())
                .execute()
            return PageImpl(content, pageable, total.toLong())
        }
    }
}