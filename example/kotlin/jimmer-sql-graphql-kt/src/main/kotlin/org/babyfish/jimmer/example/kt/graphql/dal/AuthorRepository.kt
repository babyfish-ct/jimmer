package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Repository

@Repository
class AuthorRepository(
    private val sqlClient: KSqlClient
) {

    fun find(name: String?): List<Author> =
        sqlClient.createQuery(Author::class) {
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