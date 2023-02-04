package org.babyfish.jimmer.sql.example.dal

import org.babyfish.jimmer.Static
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import kotlin.reflect.KClass

interface BookRepository : KRepository<Book, Long> {

    fun <S: Static<Book>> findBooks(
        pageable: Pageable,
        name: String?,
        storeName: String?,
        authorName: String?,
        staticType: KClass<S>
    ): Page<S> =
        pager(pageable).execute(
            sql.createQuery(Book::class) {
                name?.takeIf { it.isNotEmpty() }?.let {
                    where(table.name ilike it)
                }
                storeName?.takeIf { it.isNotEmpty() }?.let {
                    where(table.store.name ilike it)
                }
                authorName?.takeIf { it.isNotEmpty() }?.let {
                    where(
                        table.id valueIn subQuery(Author::class) {
                            where(
                                or(
                                    table.firstName ilike it,
                                    table.lastName ilike it
                                )
                            )
                            select(table.books.id)
                        }
                    )
                }
                orderBy(pageable.sort)
                select(table.fetch(staticType))
            }
        )
}