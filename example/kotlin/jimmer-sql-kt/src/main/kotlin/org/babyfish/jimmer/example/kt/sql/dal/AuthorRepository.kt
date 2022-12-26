package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher

interface AuthorRepository : KRepository<Author, Long> {

    fun findByFirstNameAndLastNameAndGender(
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        fetcher: Fetcher<Author>?
    ): List<Author>
}