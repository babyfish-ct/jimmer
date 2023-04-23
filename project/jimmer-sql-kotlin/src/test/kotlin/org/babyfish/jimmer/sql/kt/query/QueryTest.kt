package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.kt.model.classic.author.fullName2
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.*
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
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE from BOOK_STORE tb_1_")
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
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE from BOOK_STORE tb_1_")
            rows {
                expect("[Tuple3(_1=1, _2=O'REILLY, _3=null), Tuple3(_1=2, _2=MANNING, _3=null)]") {
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
                orderBy(table.edition.desc())
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
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
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |group by tb_2_.NAME""".trimMargin()
            )
            rows {
                expect("[Tuple2(_1=MANNING, _2=3), Tuple2(_1=O'REILLY, _2=9)]") {
                    it.toString()
                }
            }
        }
    }

    @Test
    fun testNativeSQL() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                select(
                    table,
                    sql(Int::class, "rank() over(order by %e desc)") {
                        expression(table.price)
                    },
                    sql(Int::class, "rank() over(partition by %e order by %e desc)") {
                        expression(table.store.id)
                        expression(table.price)
                    }
                )
            }
        ) {
            sql(
                """select 
                    |tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, 
                    |rank() over(order by tb_1_.PRICE desc), 
                    |rank() over(partition by tb_1_.STORE_ID order by tb_1_.PRICE desc) 
                    |from BOOK tb_1_""".trimMargin()
            )
            rows {
                contentEquals(
                    """[
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=9, 
                        |--->--->_3=6
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":2,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":55.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=7, 
                        |--->--->_3=4
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":3,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":51.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=8, 
                        |--->--->_3=5
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":4,
                        |--->--->--->"name":"Effective TypeScript",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":73.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=5, 
                        |--->--->_3=2
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":5,
                        |--->--->--->"name":"Effective TypeScript",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":69.00,
                        |--->--->--->"store":{"id":1}}, 
                        |--->--->_2=6, 
                        |--->--->_3=3
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":6,
                        |--->--->--->"name":"Effective TypeScript",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":88.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=1, 
                        |--->--->_3=1
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":7,
                        |--->--->--->"name":"Programming TypeScript",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":47.50,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=11, 
                        |--->--->_3=8
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":8,
                        |--->--->--->"name":"Programming TypeScript",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":45.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=12, 
                        |--->--->_3=9
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":9,
                        |--->--->--->"name":"Programming TypeScript",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":48.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2=10, 
                        |--->--->_3=7
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":10,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":80.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}, 
                        |--->--->_2=3, 
                        |--->--->_3=2
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":11,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":81.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}, 
                        |--->--->_2=2, 
                        |--->--->_3=1
                        |--->), 
                        |--->Tuple3(
                        |--->--->_1={
                        |--->--->--->"id":12,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":80.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}, 
                        |--->--->_2=3, 
                        |--->--->_3=2
                        |--->)
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testUnusedQuery() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                table.asTableEx().books
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testUsedQuery() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where(table.asTableEx().books.name ilike "sql")
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID 
                    |where lower(tb_2_.NAME) like ?""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryBySqlFormula() {
        executeAndExpect(
            sqlClient.createQuery(Author::class) {
                where(table.fullName2 eq "Alex Banks")
                select(table)
            }
        ) {
            statement(0).sql(
                """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER 
                    |from AUTHOR tb_1_ 
                    |where concat(tb_1_.FIRST_NAME, ' ', tb_1_.LAST_NAME) = ?""".trimMargin()
            )
            rows("[{\"id\":2,\"firstName\":\"Alex\",\"lastName\":\"Banks\",\"gender\":\"MALE\"}]")
        }
    }
}


