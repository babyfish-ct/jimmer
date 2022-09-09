package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.name
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.stereotype.Repository

@Repository
class BookStoreRepository(
    private val sqlClient: KSqlClient
) {

    fun find(name: String?): List<BookStore> =
        sqlClient.createQuery(BookStore::class) {
            name?.let {
                where(table.name ilike it)
            }
            select(table)
        }.execute()
}