package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike

@JvmDefaultWithCompatibility
interface AuthorRepository : KRepository<Author, Long> {

    fun findAuthors(
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        fetcher: Fetcher<Author>?
    ): List<Author> =
        sql
            .createQuery(Author::class) {
                firstName?.let {
                    where(table.firstName.ilike(it))
                }
                lastName?.let {
                    where(table.lastName.ilike(it))
                }
                gender?.let {
                    where(table.gender eq it)
                }
                orderBy(table.firstName)
                orderBy(table.lastName)
                select(table.fetch(fetcher))
            }
            .execute()
}