package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Gender
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.springframework.data.domain.Sort

interface AuthorRepository : KRepository<Author, Long> {

    fun findByFirstNameAndLastNameAndGender(
        sort: Sort,
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        fetcher: Fetcher<Author>?
    ): List<Author>
}