package org.babyfish.jimmer.example.kt.sql.controller

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BookController(
    private val sqlClient: KSqlClient
) {

    @GetMapping("/books")
    fun books(
        @RequestParam(defaultValue = "false") fetch: Boolean,
        @RequestParam(defaultValue = "") name: String,
        @RequestParam(defaultValue = "") storeName: String,
        @RequestParam(defaultValue = "") authorName: String,
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): Page<Book> {

        val query = sqlClient.createQuery(Book::class) {
            name.takeIf { it.isNotEmpty() }?.let {
                where(table.name ilike it)
            }
            storeName.takeIf { it.isNotEmpty() }?.let {
                where(table.store.name ilike it)
            }
            authorName.takeIf { it.isNotEmpty() }?.let {
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
            select(
                if (fetch) {
                    table.fetchBy {
                        allScalarFields()
                        store {
                            allScalarFields()
                            avgPrice()
                        }
                        authors {
                            allScalarFields()
                        }
                    }
                } else {
                    table
                }
            )
        }

        val rowCount = query.count()
        val pageCount = (rowCount + pageSize - 1) / pageSize
        val books = query
            .limit(limit = pageSize, offset = pageIndex * pageSize)
            .execute()
        return Page(
            books,
            totalRowCount = rowCount,
            totalPageCount = pageCount
        )
    }
}