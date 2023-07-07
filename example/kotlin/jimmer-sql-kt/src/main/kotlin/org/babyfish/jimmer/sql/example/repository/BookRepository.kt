package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

interface BookRepository : KRepository<Book, Long> {

    fun findBooks(
        pageable: Pageable,
        name: String?,
        storeName: String?,
        authorName: String?,
        fetcher: Fetcher<Book>?
    ): Page<Book> =
        pager(pageable).execute(
            sql.createQuery(Book::class) {
                name?.takeIf { it.isNotEmpty() }?.let {
                    where(table.name ilike it)
                }
                storeName?.takeIf { it.isNotEmpty() }?.let {
                    where(table.store.name ilike it)
                }
                authorName?.takeIf { it.isNotEmpty() }?.let {
                    where(
                        table.id valueIn subQuery(Author::class) {
                            where(
                                or(
                                    table.firstName ilike it,
                                    table.lastName ilike it
                                )
                            )
                            select(table.books.id)
                        }
                    )
                }
                orderBy(pageable.sort)
                select(table.fetch(fetcher))
            }
        )

    fun findAvgPriceGroupByStoreIds(storeIds: Collection<Long>): Map<Long, BigDecimal> =
        sql
            .createQuery(Book::class) {
                where(table.store.id valueIn storeIds)
                groupBy(table.store.id)
                select(
                    table.store.id,
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
                    tuple(table.name, table.edition) valueIn subQuery(Book::class) {
                        // Apply `filter` for sub query is better.
                        where(table.store.id valueIn storeIds)
                        groupBy(table.name)
                        select(
                            table.name,
                            max(table.edition).asNonNull()
                        )
                    }
                )
                select(
                    table.store.id,
                    table.id
                )
            }
            .execute()
            .groupBy({it._1}) {
                it._2
            }
}