package org.babyfish.jimmer.spring

import org.babyfish.jimmer.spring.model.Page
import org.babyfish.jimmer.sql.Input
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import kotlin.reflect.KProperty1

interface KRepository<E: Any, ID> {

    val client: KSqlClient

    fun paginate(pageIndex: Int, pageSize: Int, query: KConfigurableRootQuery<*, E>): Page<E>

    fun findById(id: ID, fetcher: Fetcher<E>? = null): E?

    fun findByIds(ids: Collection<ID>, fetcher: Fetcher<E>? = null): List<E>

    fun findMapByIds(ids: Collection<ID>, fetcher: Fetcher<E>? = null): Map<ID, E>

    fun findAll(fetcher: Fetcher<E>? = null, vararg orderedProps: KProperty1<E, *>): List<E>

    fun findPage(pageIndex: Int, pageSize: Int, fetcher: Fetcher<E>? = null, vararg orderedProps: KProperty1<E, *>): List<E>

    fun save(entity: E): E

    fun save(input: Input<E>): E

    fun delete(entity: E): Int

    fun deleteById(id: ID): Int

    fun deleteByIds(id: Collection<ID>): Int
}