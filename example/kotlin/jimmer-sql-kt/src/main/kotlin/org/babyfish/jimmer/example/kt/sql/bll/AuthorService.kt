package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.AuthorRepository
import org.babyfish.jimmer.example.kt.sql.model.Author
import org.babyfish.jimmer.example.kt.sql.model.Gender
import org.babyfish.jimmer.example.kt.sql.model.by
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorService(
    private val authorRepository: AuthorRepository
) {

    @GetMapping("/authors/simple")
    fun findSimpleAuthors(
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<@FetchBy("SIMPLE_FETCHER") Author> =
        authorRepository.findAuthors(
            firstName,
            lastName,
            gender,
            SIMPLE_FETCHER
        )

    @GetMapping("/authors/complex")
    fun findComplexAuthors(
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<@FetchBy("COMPLEX_FETCHER") Author> =
        authorRepository.findAuthors(
            firstName,
            lastName,
            gender,
            COMPLEX_FETCHER
        )

    companion object {

        @JvmStatic
        private val SIMPLE_FETCHER = newFetcher(Author::class).by {
            firstName()
            lastName()
        }

        @JvmStatic
        private val COMPLEX_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
            books {
                allScalarFields()
                tenant(false)
                store {
                    allScalarFields()
                    avgPrice()
                }
            }
        }
    }
}