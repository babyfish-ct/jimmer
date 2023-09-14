package org.babyfish.jimmer.sql.example.business

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.ThrowsAll
import org.babyfish.jimmer.sql.example.repository.BookRepository
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.model.dto.BookInput
import org.babyfish.jimmer.sql.example.model.dto.CompositeBookInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
 */
@RestController
@RequestMapping("/book")
@Transactional
class BookService(
    private val bookRepository: BookRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleBooks(): List<@FetchBy("SIMPLE_FETCHER") Book> = // ❶
        bookRepository.findAll(SIMPLE_FETCHER) {
            asc(Book::name)
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
    ): Page<@FetchBy("DEFAULT_FETCHER") Book> = // ❷
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
    ): @FetchBy("COMPLEX_FETCHER") Book? = // ❸
        bookRepository.findNullable(id, COMPLEX_FETCHER)

    @PutMapping
    @ThrowsAll(SaveErrorCode::class) // ❹
    fun saveBook(@RequestBody input: BookInput): Book = // ❺
        bookRepository.save(input)

    @PutMapping("/composite")
    @ThrowsAll(SaveErrorCode::class) // ❻
    fun saveBook(@RequestBody input: CompositeBookInput): Book = // ❼
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
        }
    }
}

/*----------------Documentation Links----------------
❶ ❷ ❸ https://babyfish-ct.github.io/jimmer/docs/spring/client/api#declare-fetchby
❹ ❻ https://babyfish-ct.github.io/jimmer/docs/spring/client/error#allow-to-throw-all-exceptions-of-family
❺ ❼ https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/input-dto/
---------------------------------------------------*/
