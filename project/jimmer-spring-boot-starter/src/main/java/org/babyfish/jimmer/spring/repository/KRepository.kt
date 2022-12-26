package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.spring.model.Input
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.springframework.core.annotation.AliasFor
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*
import kotlin.reflect.KProperty1

@NoRepositoryBean
interface KRepository<E: Any, ID: Any> : PagingAndSortingRepository<E, ID> {

    val sql: KSqlClient

    fun pager(pageIndex: Int, pageSize: Int, block: (SortDsl<E>.() -> Unit)? = null): Pager<E>

    fun pager(pageIndex: Int, pageSize: Int, sort: Sort?): Pager<E> =
        pager(PageRequest.of(pageIndex, pageSize, sort ?: Sort.unsorted()))

    fun pager(pageable: Pageable): Pager<E>

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

    fun findAll(fetcher: Fetcher<E>? = null, sort: Sort): List<E>

    override fun findAll(sort: Sort): List<E> =
        findAll(null, sort)

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

    override fun <S: E> save(entity: S): S

    override fun <S : E> saveAll(entities: Iterable<S>): List<S>

    fun save(input: Input<E>): E

    override fun delete(entity: E)

    override fun deleteById(id: ID)

    @AliasFor("deleteAllById")
    fun deleteByIds(ids: Iterable<ID>)

    @AliasFor("deleteByIds")
    override fun deleteAllById(ids: Iterable<ID>) {
        deleteByIds(ids)
    }

    override fun deleteAll()

    override fun deleteAll(entities: Iterable<E>)

    val graphql: GraphQl<E>

    interface Pager<E> {

        fun execute(query: KConfigurableRootQuery<*, E>): Page<E>
    }

    interface GraphQl<E> {

        fun <X: Any> load(prop: KProperty1<E, X?>, sources: Collection<E>): Map<E, X>
    }
}