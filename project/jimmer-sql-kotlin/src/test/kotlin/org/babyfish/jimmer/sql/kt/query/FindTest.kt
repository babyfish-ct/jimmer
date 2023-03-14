package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.by
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.store.by
import kotlin.test.Test

class FindTest : AbstractQueryTest() {

    @Test
    fun testFind() {
        connectAndExpect({
            sqlClient.entities.forConnection(it)
                .findAll(BookStore::class) {
                    desc(BookStore::name)
                }
        }) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE as tb_1_ order by tb_1_.NAME desc""".trimMargin()
            )
            rows(
                """[
                    |--->{"id":1,"name":"O'REILLY","version":0,"website":null},
                    |--->{"id":2,"name":"MANNING","version":0,"website":null}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testByFetcher() {
        connectAndExpect({
            sqlClient.entities.forConnection(it)
                .findAll(
                    newFetcher(BookStore::class).by {
                        allScalarFields()
                        books({
                            filter {
                                where(table.edition eq 3)
                            }
                        }) {
                            allScalarFields()
                        }
                    }
                ) {
                    asc(BookStore::name)
                }
        }) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE as tb_1_ 
                    |order by tb_1_.NAME asc""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK as tb_1_ 
                    |where tb_1_.STORE_ID in (?, ?) and tb_1_.EDITION = ?""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":2,
                    |--->--->"name":"MANNING",
                    |--->--->"version":0,
                    |--->--->"website":null,
                    |--->--->"books":[
                    |--->--->--->{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}
                    |--->--->]
                    |--->},{
                    |--->--->"id":1,
                    |--->--->"name":"O'REILLY",
                    |--->--->"version":0,
                    |--->--->"website":null,
                    |--->--->"books":[
                    |--->--->--->{"id":3,"name":"Learning GraphQL","edition":3,"price":51.00},
                    |--->--->--->{"id":6,"name":"Effective TypeScript","edition":3,"price":88.00},
                    |--->--->--->{"id":9,"name":"Programming TypeScript","edition":3,"price":48.00}
                    |--->--->]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun findByExample() {
        connectAndExpect({
            sqlClient.entities.forConnection(it)
                .findByExample(
                    example(
                        new(Book::class).by {
                            name = "GraphQL"
                            edition = 3
                        }
                    ) {
                        like(Book::name)
                    }
                ) {
                    asc(Book::name)
                }
        }) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.NAME like ? and tb_1_.EDITION = ? 
                    |order by tb_1_.NAME asc""".trimMargin()
            )
            variables("%GraphQL%", 3)
            rows(
                """[
                    |--->{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":3,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2}
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"store":{"id":1}
                    |--->}]""".trimMargin()
            )
        }
    }

    @Test
    fun testFindByExampleAndFetcher() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        store().apply {
                            id = 2
                        }
                    }
                ),
                newFetcher(Book::class).by {
                    allScalarFields()
                    store {
                        allScalarFields()
                    }
                }
            ) {
                desc(Book::name)
            }
        }) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.STORE_ID = ? 
                    |order by tb_1_.NAME desc""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":10,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":1,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |--->},{
                    |--->--->"id":11,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":2,
                    |--->--->"price":81.00,
                    |--->--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |--->},{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":3,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }
}