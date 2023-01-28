package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.BookRepository
import org.babyfish.jimmer.example.kt.sql.dal.BookStoreRepository
import org.babyfish.jimmer.example.kt.sql.model.Book
import org.babyfish.jimmer.example.kt.sql.model.BookStore
import org.babyfish.jimmer.example.kt.sql.model.BookStoreInput
import org.babyfish.jimmer.example.kt.sql.model.by
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bookStore")
class BookStoreService(
    private val bookStoreRepository: BookStoreRepository,
    private val bookRepository: BookRepository
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

    @GetMapping("/complexList")
    fun findComplexStores(): List<@FetchBy("COMPLEX_FETCHER") BookStore> =
        bookStoreRepository.findAll(COMPLEX_FETCHER) {
            asc(BookStore::name)
        }

    /*
     * In some cases, simple combination of different data have to be returned,
     * which cannot be achieved based on associated properties.
     *
     * For example, here we need each bookstore and its newest book,
     * not all its books: `BookStore.books`.
     *
     * This is a WEAK association at the business level.
     *
     * You can use ordinary object to assemble different entity objects
     * that are not strongly associated.
     *
     * You can use multiple `@FetchBy` annotations even if your return type
     * has multiple generic parameters, or even nested generic types.
     * This is why `@FetchBy` decorates generic parameters but not return types.
     */
    @GetMapping("/withNewestBook")
    fun findStoresWithNewestBook(): List<
        Tuple2<
            @FetchBy("SIMPLE_FETCHER") BookStore,
            @FetchBy("NEWEST_BOOK_FETCHER") Book?
        >
    > {
        val stores = bookStoreRepository.findAll(SIMPLE_FETCHER)

        // BookStoreId -> BookId
        val newestBookIdMap = stores
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let {
                bookRepository.findNewestIdsByStoreIds(it)
            } ?: emptyMap()

        // BookId -> Book
        val bookMap = newestBookIdMap
            .values
            .takeIf { it.isNotEmpty() }
            ?.let {
                bookRepository.findMapByIds(it, NEWEST_BOOK_FETCHER)
            } ?: emptyMap()

        return stores.map {
            val newestBookId = newestBookIdMap[it.id]
            val newestBook = newestBookId?.let { bookMap[it] }
            Tuple2(it, newestBook)
        }
    }

    @PutMapping
    fun saveBookStore(input: BookStoreInput): BookStore =
        bookStoreRepository.save(input)

    companion object {

        private val SIMPLE_FETCHER = newFetcher(BookStore::class).by {
            name()
        }

        private val DEFAULT_FETCHER = newFetcher(BookStore::class).by {
            allScalarFields()
        }

        private val COMPLEX_FETCHER = newFetcher(BookStore::class).by {
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

        private val NEWEST_BOOK_FETCHER = newFetcher(Book::class).by {
            allScalarFields()
            tenant(false)
            authors {
                allScalarFields()
            }
        }
    }
}