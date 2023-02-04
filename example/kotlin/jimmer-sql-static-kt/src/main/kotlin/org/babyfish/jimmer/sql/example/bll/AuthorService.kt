package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.dal.AuthorRepository
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Gender
import org.babyfish.jimmer.sql.example.model.dto.AuthorInput
import org.babyfish.jimmer.sql.example.model.dto.ComplexAuthor
import org.babyfish.jimmer.sql.example.model.dto.DefaultAuthor
import org.babyfish.jimmer.sql.example.model.dto.SimpleAuthor
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
    ): List<SimpleAuthor> =
        authorRepository.findAllStaticObjects(SimpleAuthor::class) {
            asc(Author::firstName)
            asc(Author::lastName)
        }

    @GetMapping("/list")
    fun findAuthors(
        @RequestParam(defaultValue = "firstName asc, lastName asc") sortCode: String,
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<DefaultAuthor> =
        authorRepository.findByFirstNameAndLastNameAndGender(
            SortUtils.toSort(sortCode),
            firstName,
            lastName,
            gender,
            DefaultAuthor::class
        )

    @GetMapping("/{id}")
    fun findComplexAuthor(
        @PathVariable id: Long
    ): ComplexAuthor? =
        authorRepository.findNullableStaticObject(ComplexAuthor::class, id)

    @PutMapping
    fun saveAuthor(@RequestBody input: AuthorInput): Author =
        authorRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteAuthor(@PathVariable id: Long) {
        authorRepository.deleteById(id)
    }
}