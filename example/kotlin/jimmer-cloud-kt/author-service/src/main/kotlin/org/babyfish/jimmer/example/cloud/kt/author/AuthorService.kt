package org.babyfish.jimmer.example.cloud.kt.author

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.cloud.kt.model.Author
import org.babyfish.jimmer.example.cloud.kt.model.by
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorService(
    private val authorRepository: AuthorRepository
) {

    @GetMapping("/author/{id}")
    fun findAuthor(
        @PathVariable("id") id: Long
    ): @FetchBy("SIMPLE_FETCHER") Author? =
        authorRepository.findNullable(id, SIMPLE_FETCHER)

    @GetMapping("/author/{id}/detail")
    fun findAuthorDetail(
        @PathVariable("id") id: Long
    ): @FetchBy("COMPLEX_FETCHER") Author? =
        authorRepository.findNullable(id, COMPLEX_FETCHER)

    companion object {

        val SIMPLE_FETCHER = newFetcher(Author::class).by {
            firstName()
            lastName()
        }

        val COMPLEX_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
            books {
                allScalarFields()
                store {
                    allScalarFields()
                }
            }
        }
    }
}