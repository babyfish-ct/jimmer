package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.babyfish.jimmer.sql.kt.model.classic.book.store
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test

class SubQueryTest : AbstractQueryTest() {

    @Test
    fun testAsExpression() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                val childCount = subQuery(Book::class) {
                    where(table.store eq parentTable)
                    select(count(table))
                }
                orderBy(childCount)
                select(table, childCount)
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE, 
                    |--->(
                    |--->--->select count(tb_2_.ID) 
                    |--->--->from BOOK tb_2_ 
                    |--->--->where 
                    |--->--->--->tb_2_.STORE_ID = tb_1_.ID
                    |--->) 
                    |from BOOK_STORE tb_1_ 
                    |order by (
                    |--->select count(tb_2_.ID) 
                    |--->from BOOK tb_2_ 
                    |--->where 
                    |--->--->tb_2_.STORE_ID = tb_1_.ID
                    |) asc""".trimMargin()
            )
            rows {
                contentEquals(
                    """[
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":2,
                        |--->--->--->"name":"MANNING",
                        |--->--->--->"version":0,
                        |--->--->--->"website":null
                        |--->--->}, 
                        |--->--->_2=3
                        |--->), 
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":1,
                        |--->--->--->"name":"O'REILLY",
                        |--->--->--->"version":0,
                        |--->--->--->"website":null
                        |--->--->}, 
                        |--->--->_2=9
                        |--->)
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testSingleColumnIn() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    table.store.id valueIn subQuery(BookStore::class) {
                        where(table.name.like("MAN", LikeMode.START))
                        select(table.id)
                    }
                )
                select(table)
            }
        ) {
            sql(
                """select 
                    |tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID in (
                    |--->select tb_3_.ID 
                    |--->from BOOK_STORE tb_3_ 
                    |--->where tb_3_.NAME like ?
                    |)""".trimMargin()
            )
            variables("MAN%")
            rows {
                contentEquals(
                    """[
                        |--->{
                        |--->--->"id":10,
                        |--->--->"name":"GraphQL in Action",
                        |--->--->"edition":1,
                        |--->--->"price":80.00,
                        |--->--->"store":{"id":2}
                        |--->}, {
                        |--->--->"id":11,
                        |--->--->"name":"GraphQL in Action",
                        |--->--->"edition":2,
                        |--->--->"price":81.00,
                        |--->--->"store":{"id":2}
                        |--->}, {
                        |--->--->"id":12,
                        |--->--->"name":"GraphQL in Action",
                        |--->--->"edition":3,
                        |--->--->"price":80.00,
                        |--->--->"store":{"id":2}
                        |--->}
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testMultipleColumnIn() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(tuple(table.name, table.edition) valueIn subQuery(Book::class) {
                    groupBy(table.name)
                    select(table.name, max(table.edition).asNonNull())
                })
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where (tb_1_.NAME, tb_1_.EDITION) in (
                    |--->select tb_2_.NAME, max(tb_2_.EDITION) 
                    |--->from BOOK tb_2_ 
                    |--->group by tb_2_.NAME
                    |)""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":6,
                    |--->--->"name":"Effective TypeScript",
                    |--->--->"edition":3,
                    |--->--->"price":88.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":9,
                    |--->--->"name":"Programming TypeScript",
                    |--->--->"edition":3,
                    |--->--->"price":48.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":3,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testAny() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    tuple(table.name, table.edition) eq any(
                        subQuery(Book::class) {
                            groupBy(table.name)
                            select(
                                table.name,
                                max(table.edition).asNonNull()
                            )
                        }
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where (tb_1_.NAME, tb_1_.EDITION) = any(
                    |--->select tb_2_.NAME, max(tb_2_.EDITION) 
                    |--->from BOOK tb_2_ 
                    |--->group by tb_2_.NAME
                    |)""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":6,
                    |--->--->"name":"Effective TypeScript",
                    |--->--->"edition":3,
                    |--->--->"price":88.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":9,
                    |--->--->"name":"Programming TypeScript",
                    |--->--->"edition":3,
                    |--->--->"price":48.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"edition":3,
                    |--->--->"price":80.00,
                    |--->--->"store":{"id":2}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testExists() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where(
                    notExists(
                        subQuery(Book::class) {
                            where(
                                table.store eq parentTable,
                                or(
                                    table.name ilike "type",
                                    table.name ilike "script"
                                )
                            )
                            select(constant(1))
                        }
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |where not exists(
                    |--->select 1 
                    |--->from BOOK tb_2_ 
                    |--->where 
                    |--->--->tb_2_.STORE_ID = tb_1_.ID 
                    |--->and (
                    |--->--->--->lower(tb_2_.NAME) like ? 
                    |--->--->or 
                    |--->--->--->lower(tb_2_.NAME) like ?
                    |--->)
                    |)""".trimMargin()
            )
            variables("%type%", "%script%")
            rows(
                """[{"id":2,"name":"MANNING","version":0,"website":null}]"""
            )
        }
    }

    @Test
    fun testExistsOnWildSubQuery() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where(
                    notExists(
                        wildSubQuery(Book::class) {
                            where(
                                table.store eq parentTable,
                                or(
                                    table.name ilike "type",
                                    table.name ilike "script"
                                )
                            )
                        }
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |where not exists(
                    |--->select 1 
                    |--->from BOOK tb_2_ 
                    |--->where 
                    |--->--->tb_2_.STORE_ID = tb_1_.ID 
                    |--->and (
                    |--->--->--->lower(tb_2_.NAME) like ? 
                    |--->--->or 
                    |--->--->--->lower(tb_2_.NAME) like ?
                    |--->)
                    |)""".trimMargin()
            )
            variables("%type%", "%script%")
            rows(
                """[{"id":2,"name":"MANNING","version":0,"website":null}]"""
            )
        }
    }
}