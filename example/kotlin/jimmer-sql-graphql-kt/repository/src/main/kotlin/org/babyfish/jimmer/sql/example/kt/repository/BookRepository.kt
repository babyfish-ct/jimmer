package org.babyfish.jimmer.sql.example.kt.repository

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.kt.model.*
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.table.makeOrders
import org.springframework.data.domain.Sort
import java.math.BigDecimal

interface BookRepository : KRepository<Book, Long> {

    /**
     * Manually implement complex query.
     *
     * <p>The functionality of this method is the same as the super QBE method.[find]</p>
     */
    fun findBooks(
        name: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        storeName: String?,
        authorName: String?,
        sortCode: String?
    ): List<Book> =
        sql
            .createQuery(Book::class) {
                name?.takeIf { it.isNotEmpty() }?.let {
                    where(table.name ilike it)
                }
                minPrice?.let {
                    where(table.price ge it)
                }
                maxPrice?.let {
                    where(table.price le it)
                }
                storeName?.takeIf { it.isNotEmpty() }?.let {
                    where(table.store.name ilike it)
                }
                authorName?.takeIf { it.isNotEmpty() }?.let {
                    where += table.authors {
                        or(
                            firstName ilike it,
                            lastName ilike it
                        )
                    }
                }

                orderBy(table.makeOrders(sortCode ?: "name asc"))
                select(table)
            }
            .execute()

    /**
     * Super QBE.
     *
     * <p>The functionality of this method is the same as the manual method .[findBooks]
     * </p>
     */
    fun find(
        specification: Specification<Book>,
        sort: Sort
    ): List<Book>

    fun findAvgPriceGroupByStoreIds(storeIds: Collection<Long>): Map<Long, BigDecimal> =
        sql
            .createQuery(Book::class) {
                where(table.storeId valueIn storeIds)
                groupBy(table.storeId)
                select(
                    table.storeId.asNonNull(),
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
                        where(table.storeId valueIn storeIds)
                        groupBy(table.name)
                        select(
                            table.name,
                            max(table.edition).asNonNull()
                        )
                    }
                )
                select(
                    table.storeId.asNonNull(),
                    table.id
                )
            }
            .execute()
            .groupBy({it._1}) {
                it._2
            }
}
