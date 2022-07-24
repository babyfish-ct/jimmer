package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.count
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import kotlin.test.Test
import kotlin.test.expect

class QueryTest : AbstractQueryTest() {

    @Test
    fun testTable() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(table)
            }
        ) {
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE from BOOK_STORE as tb_1_")
            rows(
                """[
                    |--->{
                    |--->--->"id":1,"name":
                    |--->--->"O'REILLY",
                    |--->--->"version":0,
                    |--->--->"website":null
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"MANNING",
                    |--->--->"version":0,
                    |--->--->"website":null
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFields() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(
                    table.id,
                    table.name,
                    table.website
                )
            }
        ) {
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE from BOOK_STORE as tb_1_")
            rows {
                expect("[Tuple3{_1=1, _2=O'REILLY, _3=null}, Tuple3{_1=2, _2=MANNING, _3=null}]") {
                    it.toString()
                }
            }
        }
    }

    @Test
    fun testFilter() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.store.name eq "MANNING")
                orderBy(table.name)
                orderBy(table.edition, OrderMode.DESC)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_2_.NAME = ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":3,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2}
                    |--->},{
                    |--->--->"id":11,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":2,
                    |--->--->"price":81.00,
                    |--->--->"store":{"id":2}
                    |--->},{
                    |--->--->"id":10,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":1,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testGroup() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                groupBy(table.store.name)
                select(table.store.name, count(table))
            }
        ) {
            sql(
                """select tb_2_.NAME, count(tb_1_.ID) 
                    |from BOOK as tb_1_ 
                    |inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |group by tb_2_.NAME""".trimMargin()
            )
            rows {
                expect("[Tuple2{_1=MANNING, _2=3}, Tuple2{_1=O'REILLY, _2=9}]") {
                    it.toString()
                }
            }
        }
    }
}


