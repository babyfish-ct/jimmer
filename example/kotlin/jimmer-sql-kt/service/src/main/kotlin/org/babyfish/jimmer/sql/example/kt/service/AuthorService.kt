package org.babyfish.jimmer.sql.example.kt.service

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.example.kt.repository.AuthorRepository
import org.babyfish.jimmer.sql.example.kt.model.Author
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.kt.model.by
import org.babyfish.jimmer.sql.example.kt.service.dto.AuthorInput
import org.babyfish.jimmer.sql.example.kt.service.dto.AuthorSpecification
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
import org.babyfish.jimmer.sql.runtime.SaveException
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/*
 * Why add spring web annotations to the service class?
 *
 * The success and popularity of rich client technologies represented by React, Vue and Angular
 * have greatly reduced the significance of the Controller layer on the spring server side.
 *
 * Moreover, over-bloated code structures are not conducive to demonstrating the capabilities
 * of the framework with small examples. Therefore, this example project no longer adheres to
 * dogmatism and directly adds spring web annotations to the service class.
 */
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
        specification: AuthorSpecification,
        @RequestParam(defaultValue = "firstName asc, lastName asc") sortCode: String
    ): List<@FetchBy("DEFAULT_FETCHER") Author> =
        authorRepository.find(
            specification,
            SortUtils.toSort(sortCode),
            DEFAULT_FETCHER
        )

    @GetMapping("/{id}")
    fun findComplexAuthor(
        @PathVariable id: Long
    ): @FetchBy("COMPLEX_FETCHER") Author? =
        authorRepository.findNullable(id, COMPLEX_FETCHER)

    @PutMapping
    @Throws(SaveException::class)
    fun saveAuthor(@RequestBody input: AuthorInput): Author =
        authorRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteAuthor(@PathVariable id: Long) {
        authorRepository.deleteById(id)
    }

    companion object {

        /**
         * Simple Book DTO
         */
        private val SIMPLE_FETCHER = newFetcher(Author::class).by {
            firstName()
            lastName()
        }

        /**
         * Default Book DTO
         */
        private val DEFAULT_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
        }

        /**
         * Complex Bok DTO
         */
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

