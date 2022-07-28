package org.babyfish.jimmer.example.kt.sql.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption


@ShellComponent
class PaginationQueryShell(
    private val sqlClient: KSqlClient,
    private val prettyWriter: ObjectWriter
) {

    @ShellMethod(
        "Find books by --name, --store-ame, --author-name, --page-size and --fetch" +
            "(Example: books --store-name M --fetch)"
    )
    fun books(
        @ShellOption(defaultValue = "") name: String,
        @ShellOption(defaultValue = "") storeName: String,
        @ShellOption(defaultValue = "") authorName: String,
        @ShellOption(defaultValue = "2") pageSize: Int,
        @ShellOption(defaultValue = "false") fetch: Boolean
    ) {
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
            orderBy(table.edition, OrderMode.DESC)
            select(
                if (fetch) {
                    table.fetchBy {
                        allScalarFields()
                        store {
                            allScalarFields()
                        }
                        authors {
                            allScalarFields()
                        }
                    }
                } else {
                    table
                },
                sql(Int::class, "rank() over(order by %e desc)") {
                    expression(table.price)
                },
                sql(Int::class, "rank() over(partition by %e order by %e desc)") {
                    expression(table.store.id)
                    expression(table.price)
                }
            )
        }

        val countQuery = query
            .reselect {
                select(count(table))
            }
            .withoutSortingAndPaging()

        val rowCount = countQuery.execute()[0]
        val pageCount = (rowCount + pageSize - 1) / pageSize
        println("----Output start---------------------------------")
        println("Total row count: $rowCount, pageCount: $pageCount")
        println("-------------------------------------------------")

        for (pageNo in 1..pageCount) {

            println("----Page no: $pageNo-----------")

            val offset = (pageSize * (pageNo - 1)).toInt()
            val rows = query.limit(pageSize, offset).execute()
            for ((book, globalRank, partitionRank) in rows) {
                println("Book: $book")
                println("Global rank: $globalRank")
                println("Partition rank: $partitionRank")
            }
        }
        println("----Output end-----------------------------------")
        println()
    }
}