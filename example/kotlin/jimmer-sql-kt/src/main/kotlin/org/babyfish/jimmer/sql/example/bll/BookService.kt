package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.example.dal.BookRepository
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.model.input.BookInput
import org.babyfish.jimmer.sql.example.model.input.CompositeBookInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/book")
@Transactional
class BookService(
    private val bookRepository: BookRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleBooks(): List<@FetchBy("SIMPLE_FETCHER") Book> =
        bookRepository.findAll(SIMPLE_FETCHER) {
            asc(Book::name)
            asc(Book::chapters)
        }

    @GetMapping("/list")
    fun findBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        // The `sortCode` also support implicit join, like `store.name asc`
        @RequestParam(defaultValue = "name asc, edition desc") sortCode: String,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<@FetchBy("DEFAULT_FETCHER") Book> =
        bookRepository.findBooks(
            PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
            name,
            storeName,
            authorName,
            DEFAULT_FETCHER
        )

    @GetMapping("/{id}")
    fun findComplexBook(
        @PathVariable id: Long,
    ): @FetchBy("COMPLEX_FETCHER") Book? =
        bookRepository.findNullable(id, COMPLEX_FETCHER)

    @PutMapping
    fun saveBook(@RequestBody input: BookInput): Book =
        bookRepository.save(input)

    @PutMapping("/withChapters")
    fun saveBook(@RequestBody input: CompositeBookInput): Book =
        bookRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: Long) {
        bookRepository.deleteById(id)
    }

    companion object {

        private val SIMPLE_FETCHER = newFetcher(Book::class).by {
            name()
        }

        private val DEFAULT_FETCHER = newFetcher(Book::class).by {

            allScalarFields()
            tenant(false)

            store {
                name()
            }
            authors {
                firstName()
                lastName()
            }
        }

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
