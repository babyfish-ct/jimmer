package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.data.domain.Page

@JvmDefaultWithCompatibility
interface BookRepository : KRepository<Book, Long> {

    fun findBooks(
        pageIndex: Int,
        pageSize: Int,
        name: String?,
        storeName: String?,
        authorName: String?,
        fetcher: Fetcher<Book>?
    ): Page<Book> =
        pager(pageIndex, pageSize)
            .execute(
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
                    orderBy(table.name)
                    orderBy(table.edition.desc())
                    select(table.fetch(fetcher))
                }
            )
}