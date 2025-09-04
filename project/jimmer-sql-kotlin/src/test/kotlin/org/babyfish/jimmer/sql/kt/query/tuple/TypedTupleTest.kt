package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.books
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookViewForTupleTest
import kotlin.test.Test

class TypedTupleTest : AbstractQueryTest() {

    @Test
    fun testAggregateTuple() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                groupBy(table.storeId)
                select(
                    AggregateTupleMapper
                        .storeId(table.storeId.asNonNull())
                        .bookCount(rowCount())
                        .minPrice(min(table.price))
                        .maxPrice(max(table.price))
                        .avgPrice(avgAsDecimal(table.price))
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.STORE_ID, 
                    |--->count(1), 
                    |--->min(tb_1_.PRICE), 
                    |--->max(tb_1_.PRICE), 
                    |--->avg(tb_1_.PRICE) 
                    |from BOOK tb_1_ 
                    |group by tb_1_.STORE_ID""".trimMargin()
            )
            rows(
                """[{
                    |--->"storeId":1,
                    |--->"bookCount":9,
                    |--->"minPrice":45.0,
                    |--->"maxPrice":88.0,
                    |--->"avgPrice":58.5
                    |},{
                    |--->"storeId":2,
                    |--->"bookCount":3,
                    |--->"minPrice":80.0,
                    |--->"maxPrice":81.0,
                    |--->"avgPrice":80.333333333333
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testEntityTuple() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.edition eq 3)
                select(
                    EntityTupleMapper
                        .book(
                            table.fetchBy {
                                name()
                                store {
                                    name()
                                }
                            }
                        )
                        .authorCount(
                            subQuery(Author::class) {
                                where(table.books.id eq parentTable.id)
                                selectCount()
                            }
                        )
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, 
                    |--->(
                    |--->--->select count(1) from AUTHOR tb_2_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                    |--->--->where tb_3_.BOOK_ID = tb_1_.ID
                    |--->) 
                    |--->from BOOK tb_1_ 
                    |--->where tb_1_.EDITION = ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from BOOK_STORE tb_1_ 
                    |where tb_1_.ID in (?, ?)""".trimMargin()
            )
            rows(
                """[{
                    |--->"book":{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":2
                    |},{
                    |--->"book":{
                    |--->--->"id":6,
                    |--->--->"name":"Effective TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":9,
                    |--->--->"name":"Programming TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"store":{"id":2,"name":"MANNING"}
                    |--->},
                    |--->"authorCount":1
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testEntityTupleWithJoinFetch() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.edition eq 3)
                select(
                    EntityTupleMapper
                        .book(
                            table.fetchBy {
                                name()
                                store(ReferenceFetchType.JOIN_ALWAYS) {
                                    name()
                                }
                            }
                        )
                        .authorCount(
                            subQuery(Author::class) {
                                where(table.books.id eq parentTable.id)
                                selectCount()
                            }
                        )
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_2_.ID, tb_2_.NAME, 
                    |--->(
                    |--->--->select count(1) 
                    |--->--->from AUTHOR tb_3_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID 
                    |--->--->where tb_4_.BOOK_ID = tb_1_.ID
                    |--->) 
                    |--->from BOOK tb_1_ 
                    |--->left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |--->where tb_1_.EDITION = ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"book":{
                    |--->--->"id":6,
                    |--->--->"name":"Effective TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"store":{"id":2,"name":"MANNING"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":2
                    |},{
                    |--->"book":{
                    |--->--->"id":9,
                    |--->--->"name":"Programming TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testDtoTuple() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.edition eq 3)
                select(
                    DtoTupleMapper
                        .book(
                            table.fetch(BookViewForTupleTest::class)
                        )
                        .authorCount(
                            subQuery(Author::class) {
                                where(table.books.id eq parentTable.id)
                                selectCount()
                            }
                        )
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, 
                    |--->(
                    |--->--->select count(1) from AUTHOR tb_2_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                    |--->--->where tb_3_.BOOK_ID = tb_1_.ID
                    |--->) 
                    |--->from BOOK tb_1_ 
                    |--->where tb_1_.EDITION = ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from BOOK_STORE tb_1_ 
                    |where tb_1_.ID in (?, ?)""".trimMargin()
            )
            rows(
                """[{
                    |--->"book":{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":2
                    |},{
                    |--->"book":{
                    |--->--->"id":6,
                    |--->--->"name":"Effective TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":9,
                    |--->--->"name":"Programming TypeScript",
                    |--->--->"store":{"id":1,"name":"O'REILLY"}
                    |--->},
                    |--->"authorCount":1
                    |},{
                    |--->"book":{
                    |--->--->"id":12,
                    |--->--->"name":"GraphQL in Action",
                    |--->--->"store":{"id":2,"name":"MANNING"}
                    |--->},
                    |--->"authorCount":1
                    |}]""".trimMargin()
            )
        }
    }
}