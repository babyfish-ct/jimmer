package org.babyfish.jimmer.sql.example.dal

import org.babyfish.jimmer.Static
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Gender
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.springframework.data.domain.Sort
import kotlin.reflect.KClass

interface AuthorRepository : KRepository<Author, Long> {

    fun <S: Static<Author>> findByFirstNameAndLastNameAndGender(
        sort: Sort,
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        staticType: KClass<S>
    ): List<S>
}