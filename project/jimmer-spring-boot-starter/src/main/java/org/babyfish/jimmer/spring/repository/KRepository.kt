package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.spring.model.Input
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import kotlin.reflect.KProperty1

@NoRepositoryBean
interface KRepository<E: Any, ID> : Repository<E, ID> {

    val sql: KSqlClient

    fun page(pageIndex: Int, pageSize: Int, query: KConfigurableRootQuery<*, E>): Page<E>

    fun findById(id: ID, fetcher: Fetcher<E>? = null): E?

    fun findByIds(ids: Collection<ID>, fetcher: Fetcher<E>? = null): List<E>

    fun findMapByIds(ids: Collection<ID>, fetcher: Fetcher<E>? = null): Map<ID, E>

    fun findAll(fetcher: Fetcher<E>? = null, vararg orderedProps: KProperty1<E, *>): List<E>

    fun findAll(fetcher: Fetcher<E>? = null, orderedProps: Collection<KProperty1<E, *>>): List<E>

    fun findAll(fetcher: Fetcher<E>? = null, sort: Sort): List<E>

    fun findPage(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>? = null, vararg orderedProps: KProperty1<E, *>): Page<E>

    fun findPage(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>? = null, orderedProps: Collection<KProperty1<E, *>>): Page<E>

    fun findPage(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>? = null, sort: Sort): Page<E>

    fun save(entity: E): E

    fun save(input: Input<E>): E

    fun delete(entity: E): Int

    fun deleteById(id: ID): Int

    fun deleteByIds(id: Collection<ID>): Int
}