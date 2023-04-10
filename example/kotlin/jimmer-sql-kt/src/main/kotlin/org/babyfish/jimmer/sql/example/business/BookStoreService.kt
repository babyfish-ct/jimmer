package org.babyfish.jimmer.sql.example.business

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.ThrowsAll
import org.babyfish.jimmer.sql.example.repository.BookStoreRepository
import org.babyfish.jimmer.sql.example.model.BookStore
import org.babyfish.jimmer.sql.example.model.by
import org.babyfish.jimmer.sql.example.model.input.BookStoreInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
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
@RequestMapping("/bookStore")
@Transactional
class BookStoreService(
    private val bookStoreRepository: BookStoreRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleStores(): List<@FetchBy("SIMPLE_FETCHER") BookStore> =
        bookStoreRepository.findAll(SIMPLE_FETCHER) {
            asc(BookStore::name)
        }

    @GetMapping("/list")
    fun findStores(): List<@FetchBy("DEFAULT_FETCHER") BookStore> =
        bookStoreRepository.findAll(DEFAULT_FETCHER) {
            asc(BookStore::name)
        }

    @GetMapping("/{id}/withAllBooks")
    fun findComplexStoreWithAllBooks(
        @PathVariable id: Long
    ): @FetchBy("WITH_ALL_BOOKS_FETCHER") BookStore? =
        bookStoreRepository.findNullable(id, WITH_ALL_BOOKS_FETCHER)

    @GetMapping("/{id}/withNewestBooks")
    fun findComplexStoreWithNewestBooks(
        @PathVariable id: Long
    ): @FetchBy("WITH_NEWEST_BOOKS_FETCHER") BookStore? =
        bookStoreRepository.findNullable(id, WITH_NEWEST_BOOKS_FETCHER)

    @PutMapping
    @ThrowsAll(SaveErrorCode::class)
    fun saveBookStore(@RequestBody input: BookStoreInput): BookStore =
        bookStoreRepository.save(input)

    @DeleteMapping("{id}")
    fun deleteBookStore(@PathVariable id: Long) {
        bookStoreRepository.deleteById(id)
    }

    companion object {

        private val SIMPLE_FETCHER = newFetcher(BookStore::class).by {
            name()
        }

        private val DEFAULT_FETCHER = newFetcher(BookStore::class).by {
            allScalarFields()
        }

        private val WITH_ALL_BOOKS_FETCHER = newFetcher(BookStore::class).by {
            allScalarFields()
            avgPrice()
            books {
                allScalarFields()
                tenant(false)
                authors {
                    allScalarFields()
                }
            }
        }

        private val WITH_NEWEST_BOOKS_FETCHER = newFetcher(BookStore::class).by {
            allScalarFields()
            avgPrice()
            newestBooks {
                allScalarFields()
                tenant(false)
                authors {
                    allScalarFields()
                }
            }
        }
    }
}