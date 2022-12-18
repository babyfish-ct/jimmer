package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.BookRepository
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.example.kt.sql.model.input.BookInput
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
    ): Page<@FetchBy("SIMPLE_BOOK_FETCHER") Book> =
        bookRepository.findBooks(
            pageIndex,
            pageSize,
            name,
            storeName,
            authorName,
            SIMPLE_BOOK_FETCHER
        )

    @GetMapping("/books/complex")
    fun findComplexBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<@FetchBy("COMPLEX_BOOK_FETCHER") Book> =
        bookRepository.findBooks(
            pageIndex,
            pageSize,
            name,
            storeName,
            authorName,
            COMPLEX_BOOK_FETCHER
        )

    @Transactional
    @PutMapping("/book")
    fun saveBook(@RequestBody input: BookInput): Book =
        bookRepository.save(input)

    @Transactional
    @PutMapping("/book/dynamic")
    fun saveBook(@RequestBody book: Book): Book =
        bookRepository.save(book)

    companion object {

        @JvmStatic
        private val SIMPLE_BOOK_FETCHER = newFetcher(Book::class).by {
            name()
        }

        @JvmStatic
        private val COMPLEX_BOOK_FETCHER = newFetcher(Book::class).by {
            allScalarFields()
            store {
                allScalarFields()
                avgPrice()
            }
            authors {
                allScalarFields()
                gender(false)
            }
        }
    }
}
