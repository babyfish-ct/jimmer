package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class BookRepository(
    private val sqlClient: KSqlClient
) {

    fun find(
        name: String?,
        storeName: String?,
        authorName: String?
    ): List<Book> =
        sqlClient.createQuery(Book::class) {
            name?.let {
                where(table.name.ilike(it, LikeMode.START))
            }
            storeName?.let {
                where(table.store.name.ilike(it, LikeMode.START))
            }
            authorName?.let {
                where(
                    exists(
                        wildSubQuery(Author::class) {
                            where(
                                table.books eq parentTable,
                                or(
                                    table.firstName.ilike(it, LikeMode.START),
                                    table.lastName.ilike(it, LikeMode.START)
                                )
                            )
                        }
                    )
                )
            }
            select(table)
        }.execute()
}