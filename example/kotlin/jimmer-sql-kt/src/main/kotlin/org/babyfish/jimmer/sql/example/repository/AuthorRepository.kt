package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.createdTime
import org.babyfish.jimmer.sql.example.model.dto.AuthorSpecification
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ge
import org.babyfish.jimmer.sql.kt.ast.expression.lt
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.springframework.data.domain.Sort

interface AuthorRepository : KRepository<Author, Long> { // ❶

    fun find( // ❷
        specification: AuthorSpecification,
        sort: Sort,
        fetcher: Fetcher<Author>?
    ): List<Author> =
        sql
            .createQuery(Author::class) {
                where(
                    table eq example(specification.toEntity()) { // ❸
                        ilike(Author::firstName)
                        ilike(Author::lastName)
                    }
                )
                specification.minCreatedTime?.let { // ❹
                    where(table.createdTime ge it)
                }
                specification.maxCreatedTimeExclusive?.let { // ❺
                    where(table.createdTime lt it)
                }
                orderBy(sort) // ❻
                select(table.fetch(fetcher)) // ❼
            }
            .execute()
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept
❷ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default
❸ https://babyfish-ct.github.io/jimmer/docs/query/qbe
❹ ❺ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-where
❻ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-order
❼ https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/
---------------------------------------------------*/