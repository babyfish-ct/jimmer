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
                    where(
                        table.id valueIn subQuery(Author::class) { // ❼
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
