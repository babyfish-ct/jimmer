package org.babyfish.jimmer.example.kt.sql.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.example.kt.sql.dal.AuthorRepository
import org.babyfish.jimmer.example.kt.sql.model.Author
import org.babyfish.jimmer.example.kt.sql.model.AuthorInput
import org.babyfish.jimmer.example.kt.sql.model.Gender
import org.babyfish.jimmer.example.kt.sql.model.by
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/author")
@Transactional
class AuthorService(
    private val authorRepository: AuthorRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleAuthors(
    ): List<@FetchBy("SIMPLE_FETCHER") Author> =
        authorRepository.findAll(SIMPLE_FETCHER) {
            asc(Author::firstName)
            asc(Author::lastName)
        }

    @GetMapping("/list")
    fun findAuthors(
        @RequestParam(defaultValue = "firstName asc, lastName asc") sortCode: String,
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<@FetchBy("DEFAULT_FETCHER") Author> =
        authorRepository.findByFirstNameAndLastNameAndGender(
            SortUtils.toSort(sortCode),
            firstName,
            lastName,
            gender,
            DEFAULT_FETCHER
        )

    @GetMapping("/authors/complex")
    fun findComplexAuthors(
        @RequestParam(defaultValue = "firstName asc, lastName asc") sortCode: String,
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<@FetchBy("COMPLEX_FETCHER") Author> =
        authorRepository.findByFirstNameAndLastNameAndGender(
            SortUtils.toSort(sortCode),
            firstName,
            lastName,
            gender,
            COMPLEX_FETCHER
        )

    @PutMapping
    fun saveAuthor(input: AuthorInput): Author =
        authorRepository.save(input)

    companion object {

        @JvmStatic
        private val SIMPLE_FETCHER = newFetcher(Author::class).by {
            firstName()
            lastName()
        }

        @JvmStatic
        private val DEFAULT_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
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