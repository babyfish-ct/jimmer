package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or

interface AuthorRepository : KRepository<Author, Long> {

    fun find(name: String?): List<Author> =
        sql.createQuery(Author::class) {
            name?.let {
                where(
                    or(
                        table.firstName ilike it,
                        table.lastName ilike it
                    )
                )
            }
            select(table)
        }.execute()
}