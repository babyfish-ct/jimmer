package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.BookRepository
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Page
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
class BookService(
    private val sqlClient: KSqlClient,
    private val bookRepository: BookRepository
) {

    @GetMapping("/stores")
    fun stores(
        @RequestParam(defaultValue = "false") fetch: Boolean,
    ): List<BookStore> =
        sqlClient.entities.findAll(
            newFetcher(BookStore::class).by {
                allScalarFields()
                if (fetch) {
                    avgPrice()
                    books {
                        allScalarFields()
                        authors {
                            allScalarFields()
                        }
                    }
                }
            }
        ) {
            asc(BookStore::name)
        }

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

    @GetMapping("/authors")
    fun authors(
        @RequestParam(defaultValue = "false") fetch: Boolean,
        @RequestParam(defaultValue = "") firstName: String,
        @RequestParam(defaultValue = "") lastName: String,
        @RequestParam gender: Gender?,
    ): List<Author> =
        new(Author::class).by {
            firstName.takeIf { it.isNotEmpty() }?.let { this.firstName = it }
            lastName.takeIf { it.isNotEmpty() }?.let { this.lastName = it }
            gender?.let { this.gender = it }
        }.let {
            sqlClient.entities.findByExample(
                example(it) {
                    ilike(Author::firstName)
                    ilike(Author::lastName)
                },
                if (fetch) {
                    newFetcher(Author::class).by {
                        allScalarFields()
                        books {
                            allScalarFields()
                            store {
                                allScalarFields()
                                avgPrice()
                            }
                        }
                    }
                } else {
                    null
                }
            ) {
                asc(Author::firstName)
                asc(Author::lastName)
            }
        }

    @Transactional
    @PutMapping("/book")
    fun saveBook(@RequestBody book: Book): Book =
        sqlClient.entities.save(book) {
            setAutoAttachingAll()
        }.modifiedEntity

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
            }
            authors {
                allScalarFields()
                gender(false)
            }
        }
    }
}
