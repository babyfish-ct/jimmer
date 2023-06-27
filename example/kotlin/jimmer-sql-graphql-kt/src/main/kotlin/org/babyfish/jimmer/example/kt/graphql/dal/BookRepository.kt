package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.*
import java.math.BigDecimal

interface BookRepository : KRepository<Book, Long> {

    fun find(
        name: String?,
        storeName: String?,
        authorName: String?
    ): List<Book> =
        sql.createQuery(Book::class) {
            name?.let {
                where(table.name ilike it)
            }
            storeName?.let {
                where(table.store.name ilike it)
            }
            authorName?.let {
                where(
                    exists(
                        wildSubQuery(Author::class) {
                            where(
                                table.books eq parentTable,
                                or(
                                    table.firstName ilike it,
                                    table.lastName ilike it
                                )
                            )
                        }
                    )
                )
            }
            orderBy(table.name)
            orderBy(table.edition.desc())
            select(table)
        }.execute()

    fun findAvgPriceGroupByStoreIds(storeIds: Collection<Long>): Map<Long, BigDecimal> =
        sql
            .createQuery(Book::class) {
                where(table.store.id valueIn storeIds)
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