package org.babyfish.jimmer.example.kt.graphql.controller

import org.babyfish.jimmer.example.kt.graphql.dal.BookRepository
import org.babyfish.jimmer.example.kt.graphql.dal.BookStoreRepository
import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.edition
import org.babyfish.jimmer.example.kt.graphql.entities.name
import org.babyfish.jimmer.example.kt.graphql.input.BookStoreInput
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Controller
class BookStoreController(
    private val sqlClient: KSqlClient,
    private val bookStoreRepository: BookStoreRepository,
    private val bookRepository: BookRepository
) {

    // --- Query ---

    @QueryMapping
    fun bookStores(
        @Argument name: String?
    ): List<BookStore> {
        return bookStoreRepository.find(name)
    }

    // --- Association ---

    @BatchMapping
    fun books(
        // Must use `java.util.List` because Spring-GraphQL has a bug: #454
        stores: java.util.List<BookStore>
    ): Map<BookStore, List<Book>> =
        sqlClient
            .getListLoader(BookStore::books)
            .forFilter {
                orderBy(table.name)
                orderBy(table.edition, OrderMode.DESC)
            }
            .batchLoad(stores)

    // --- Calculation ---

    @BatchMapping
    fun avgPrice(
        // Must use `java.util.List` because bug of Spring-GraphQL: #454
        stores: java.util.List<BookStore>
    ): Map<BookStore, BigDecimal?> {
        val avgPriceMap = bookRepository
            .findAvgPricesByStoreIds(stores.map { it.id })
        return stores.associateBy({it}) {
            avgPriceMap[it.id]
        }.filterValues {
            it !== null
        }
    }

    // --- Mutation ---
    @MutationMapping
    @Transactional
    fun saveBookStore(@Argument input: BookStoreInput): BookStore =
        sqlClient
            .entities
            .save(input.toBookStore())
            .modifiedEntity

    @MutationMapping
    @Transactional
    fun deleteBookStore(id: Long): Int {
        return sqlClient
            .entities
            .delete(BookStore::class, id)
            .affectedRowCount(BookStore::class)
    }
}