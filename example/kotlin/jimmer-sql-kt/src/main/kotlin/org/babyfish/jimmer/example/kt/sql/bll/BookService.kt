package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.BookRepository
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Page
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
class BookService(
    private val bookRepository: BookRepository
) {

    @GetMapping("/books/simple")
    fun findSimpleBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<@FetchBy("SIMPLE_FETCHER") Book> =
        bookRepository.findBooks(
            pageIndex,
            pageSize,
            name,
            storeName,
            authorName,
            SIMPLE_FETCHER
        )

    @GetMapping("/books/complex")
    fun findComplexBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<@FetchBy("COMPLEX_FETCHER") Book> =
        bookRepository.findBooks(
            pageIndex,
            pageSize,
            name,
            storeName,
            authorName,
            COMPLEX_FETCHER
        )

    /*
     * Recommend
     *
     * The save command can save arbitrarily complex data structures,
     * which is too powerful and should be sealed inside the service and not exposed.
     *
     * You should accept static Input DTO parameter, convert it to a
     * dynamic data structure and save it.
     * Unlike output DTOs, input DTOs don't have explosion issues.
     */
    @Transactional
    @PutMapping("/book")
    fun saveBook(@RequestBody input: BookInput): Book =
        bookRepository.save(input)

    /*
     * Recommend
     *
     * The save command can save arbitrarily complex data structures,
     * which is too powerful and should be sealed inside the service and not exposed.
     *
     * You should accept static Input DTO parameter, convert it to a
     * dynamic data structure and save it.
     * Unlike output DTOs, input DTOs don't have explosion issues.
     */
    @Transactional
    @PutMapping("/book/withChapters")
    fun saveBook(@RequestBody input: CompositeBookInput): Book =
        bookRepository.save(input)

    /*
     * Not recommended.
     *
     * Since the save command can save arbitrarily complex data structure,
     * it is `too powerful`, and direct exposure will cause serious security problems,
     * unless your client is an internal system and absolutely reliable.
     */
    @Transactional
    @PutMapping("/book/dynamic")
    fun saveBook(@RequestBody book: Book): Book =
        bookRepository.save(book)

    companion object {

        @JvmStatic
        private val SIMPLE_FETCHER = newFetcher(Book::class).by {
            name()
        }

        @JvmStatic
        private val COMPLEX_FETCHER = newFetcher(Book::class).by {

            allScalarFields()
            tenant(false)

            store {
                allScalarFields()
                avgPrice()
            }
            authors {
                allScalarFields()
            }
            chapters {
                allScalarFields()
            }
        }
    }
}
