package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.SpringOrders
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

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

    fun findNewestIdsByStoreIds(storeIds: Collection<Long>): Map<Long, Long> =
        sql
            .createQuery(Book::class) {
                where(table.store.id valueIn storeIds)
                groupBy(table.store.id)
                select(
                    table.store.id,
                    max(table.id).asNonNull()
                )
            }
            .execute()
            .associateBy({it._1}) {
                it._2
            }
}