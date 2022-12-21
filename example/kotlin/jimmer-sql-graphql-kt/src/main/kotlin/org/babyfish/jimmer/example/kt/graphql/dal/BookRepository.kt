package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.*

interface BookRepository : KRepository<Book, Long> {

    fun find(
        name: String?,
        storeName: String?,
        authorName: String?
    ): List<Book> =
        sql.createQuery(Book::class) {
            name?.let {
                where(table.name ilike it)
            }
            storeName?.let {
                where(table.store.name ilike it)
            }
            authorName?.let {
                where(
                    exists(
                        wildSubQuery(Author::class) {
                            where(
                                table.books eq parentTable,
                                or(
                                    table.firstName ilike it,
                                    table.lastName ilike it
                                )
                            )
                        }
                    )
                )
            }
            select(table)
        }.execute()
}