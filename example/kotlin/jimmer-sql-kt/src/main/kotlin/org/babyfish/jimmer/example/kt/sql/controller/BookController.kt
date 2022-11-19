package org.babyfish.jimmer.example.kt.sql.controller

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
class BookController(
    private val sqlClient: KSqlClient
) {

    @GetMapping("/stores")
    fun stores(
        @RequestParam(defaultValue = "false") fetch: Boolean,
    ): List<BookStore> =
        sqlClient.entities.findAll(
            newFetcher(BookStore::class).by {
                allScalarFields()
                if (fetch) {
                    avgPrice()
                    books {
                        allScalarFields()
                        authors {
                            allScalarFields()
                        }
                    }
                }
            }
        ) {
            asc(BookStore::name)
        }

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

    @GetMapping("/authors")
    fun authors(
        @RequestParam(defaultValue = "false") fetch: Boolean,
        @RequestParam(defaultValue = "") firstName: String,
        @RequestParam(defaultValue = "") lastName: String,
        @RequestParam gender: Gender?,
    ): List<Author> =
        new(Author::class).by {
            firstName.takeIf { it.isNotEmpty() }?.let { this.firstName = it }
            lastName.takeIf { it.isNotEmpty() }?.let { this.lastName = it }
            gender?.let { this.gender = it }
        }.let {
            sqlClient.entities.findByExample(
                example(it) {
                    ilike(Author::firstName)
                    ilike(Author::lastName)
                },
                if (fetch) {
                    newFetcher(Author::class).by {
                        allScalarFields()
                        books {
                            allScalarFields()
                            store {
                                allScalarFields()
                                avgPrice()
                            }
                        }
                    }
                } else {
                    null
                }
            ) {
                asc(Author::firstName)
                asc(Author::lastName)
            }
        }

    @Transactional
    @PutMapping("/book")
    fun saveBook(@RequestBody book: Book): Book =
        sqlClient.entities.save(book).modifiedEntity
}
