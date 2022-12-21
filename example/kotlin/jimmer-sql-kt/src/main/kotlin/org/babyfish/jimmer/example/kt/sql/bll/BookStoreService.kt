package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.BookStoreRepository
import org.babyfish.jimmer.example.kt.sql.model.BookStore
import org.babyfish.jimmer.example.kt.sql.model.by
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookStoreService(
    private val bookStoreRepository: BookStoreRepository
) {

    @GetMapping("/stores/simple")
    fun findSimpleStores(): List<@FetchBy("SIMPLE_FETCHER") BookStore> =
        bookStoreRepository.findAll(SIMPLE_FETCHER) {
            asc(BookStore::name)
        }

    @GetMapping("/stores/complex")
    fun findComplexStores(): List<@FetchBy("COMPLEX_FETCHER") BookStore> =
        bookStoreRepository.findAll(COMPLEX_FETCHER) {
            asc(BookStore::name)
        }

    companion object {

        @JvmStatic
        private val SIMPLE_FETCHER = newFetcher(BookStore::class).by {
            name()
        }

        @JvmStatic
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
    }
}