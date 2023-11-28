package org.babyfish.jimmer.sql.kt.api

import org.babyfish.jimmer.client.Api
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.CompositeBookInput
import java.math.BigDecimal

/**
 * An interface to test generation of `META-INF/jimmer/client`
 */
@Api(groups = ["abc"])
interface BookService {

    /**
     * Find books with simple format
     * @param name Optional value to filter `name`
     * @param edition Optional value to filter `edition`
     * @param minPrice Optional value to filter `price`
     * @param maxPrice Optional value to filter `price`
     * @param storeName Optional value to filter `store.name`
     * @param authorName Optional value to filter `Author.firstName` or `Author.lastName`
     * @return A list of books objects, with long associations
     */
    fun findSimpleBooks(
        name: String?,
        edition: Int?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        storeName: String?,
        authorName: String?
    ): List<@FetchBy("SIMPLE_FETCHER") Book>

    /**
     * Find books with simple format
     * @param name Optional value to filter `name`
     * @param edition Optional value to filter `edition`
     * @param minPrice Optional value to filter `price`
     * @param maxPrice Optional value to filter `price`
     * @param storeName Optional value to filter `store.name`
     * @param authorName Optional value to filter `Author.firstName` or `Author.lastName`
     * @return A list of books objects, with long associations
     */
    fun findComplexBooks(
        name: String?,
        edition: Int,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        storeName: String?,
        authorName: String?
    ): List<@FetchBy("COMPLEX_FETCHER") Book>

    fun saveBook(input: BookInput)

    fun saveBook(input: CompositeBookInput)

    companion object {

        val SIMPLE_FETCHER: Fetcher<Book> =
            newFetcher(Book::class).by {
                allScalarFields()
                store()
                authors()
            }

        val COMPLEX_FETCHER =
            newFetcher(Book::class).by {
                allScalarFields()
                store()
                authors()
            }
    }
}