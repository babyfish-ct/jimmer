package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.example.dal.AuthorRepository
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Gender
import org.babyfish.jimmer.sql.example.model.by
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.model.dto.AuthorInput
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

    @GetMapping("/{id}")
    fun findComplexAuthor(
        @PathVariable id: Long
    ): @FetchBy("COMPLEX_FETCHER") Author? =
        authorRepository.findNullable(id, COMPLEX_FETCHER)

    @PutMapping
    fun saveAuthor(@RequestBody input: AuthorInput): Author =
        authorRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteAuthor(@PathVariable id: Long) {
        authorRepository.deleteById(id)
    }

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