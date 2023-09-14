package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.fetchPage
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

interface BookRepository : KRepository<Book, Long> { // ❶

    fun findBooks( // ❷
        pageable: Pageable,
        name: String?,
        storeName: String?,
        authorName: String?,
        fetcher: Fetcher<Book>?
    ): Page<Book> =
        sql
            .createQuery(Book::class) {
                name?.takeIf { it.isNotEmpty() }?.let { // ❸
                    where(table.name ilike it)
                }
                storeName?.takeIf { it.isNotEmpty() }?.let { // ❹
                    where(table.store.name ilike it) // ❺
                }
                authorName?.takeIf { it.isNotEmpty() }?.let { // ❻
                    where(
                        table.id valueIn subQuery(Author::class) { // ❼
                            where(
                                or(
                                    table.firstName ilike it,
                                    table.lastName ilike it
                                )
                            )
                            select(table.books.id)// ❽
                        }
                    )
                }
                orderBy(pageable.sort) // ❾
                select(table.fetch(fetcher)) // ❿
            }
            .fetchPage(pageable) // ⓫

    fun findAvgPriceGroupByStoreIds(storeIds: Collection<Long>): Map<Long, BigDecimal> =
        sql
            .createQuery(Book::class) {
                where(table.store.id valueIn storeIds) // ⓬
                groupBy(table.store.id) // ⓭
                select(
                    table.store.id, // ⓮
                    avg(table.price).asNonNull()
                )
            }
            .execute()
            .associateBy({it._1}) {
                it._2
            }

    fun findNewestIdsGroupByStoreIds(storeIds: Collection<Long>): Map<Long, List<Long>> =
        sql
            .createQuery(Book::class) {
                where(
                    tuple(table.name, table.edition) valueIn subQuery(Book::class) {// ⓯
                        // Apply `filter` for sub query is better.
                        where(table.store.id valueIn storeIds) // ⓰
                        groupBy(table.name)
                        select(
                            table.name,
                            max(table.edition).asNonNull()
                        )
                    }
                )
                select(
                    table.store.id, // ⓱
                    table.id
                )
            }
            .execute()
            .groupBy({it._1}) {
                it._2
            }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept

❷ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default

❸ ❹ ❻ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-where

❺ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-join/

❼ ⓯ https://babyfish-ct.github.io/jimmer/docs/query/sub-query

❽ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-join/optimization#half-joins

❾ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-order

❿ https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/

⓫ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default#pagination
  https://babyfish-ct.github.io/jimmer/docs/query/paging/

⓬ ⓭ ⓮ ⓰ ⓱ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-join/optimization#ghost-joins
---------------------------------------------------*/
