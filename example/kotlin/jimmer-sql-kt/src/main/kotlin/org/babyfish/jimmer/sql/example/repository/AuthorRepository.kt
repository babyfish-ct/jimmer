package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.dto.AuthorSpecification
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.springframework.data.domain.Sort

interface AuthorRepository : KRepository<Author, Long> {

    fun find(
        specification: AuthorSpecification,
        sort: Sort,
        fetcher: Fetcher<Author>?
    ): List<Author> =
        sql
            .createQuery(Author::class) {
                where(
                    table eq example(specification.toEntity()) {
                        ilike(Author::firstName)
                        ilike(Author::lastName)
                    }
                )
                orderBy(sort)
                select(table.fetch(fetcher))
            }
            .execute()
}