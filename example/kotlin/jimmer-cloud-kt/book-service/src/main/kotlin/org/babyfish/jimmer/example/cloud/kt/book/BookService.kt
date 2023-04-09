package org.babyfish.jimmer.example.cloud.kt.book

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.cloud.kt.model.Book
import org.babyfish.jimmer.example.cloud.kt.model.by
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class BookService(
    private val bookRepository: BookRepository
) {

    @GetMapping("/book/{id}")
    fun findBook(
        @PathVariable("id") id: Long
    ) : @FetchBy("SIMPLE_FETCHER") Book? =
        bookRepository.findNullable(id, SIMPLE_FETCHER)

    @GetMapping("/book/{id}/detail")
    fun findBookDetail(
        @PathVariable("id") id: Long
    ) : @FetchBy("COMPLEX_FETCHER") Book? =
        bookRepository.findNullable(id, COMPLEX_FETCHER)

    companion object {

        val SIMPLE_FETCHER = newFetcher(Book::class).by {
            name()
            edition()
        }

        val COMPLEX_FETCHER = newFetcher(Book::class).by {
            allScalarFields()
            store {
                allScalarFields()
            }
            authors {
                allScalarFields()
            }
        }
    }
}