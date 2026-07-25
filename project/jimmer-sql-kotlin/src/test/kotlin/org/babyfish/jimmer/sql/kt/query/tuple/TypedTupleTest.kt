package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.query.cteBaseTableSymbol
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.books
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookViewForTupleTest
import kotlin.test.Test

class TypedTupleTest : AbstractQueryTest() {

    @Test
    fun testAggregateTupleAsBaseTable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
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
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table.bookCount gt 2L)
                orderBy(table.storeId)
                select(table.storeId, table.avgPrice)
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c3 
                    |from (
                    |--->select 
                    |--->--->tb_2_.STORE_ID c1, 
                    |--->--->count(1) c2, 
                    |--->--->avg(tb_2_.PRICE) c3 
                    |--->from BOOK tb_2_ 
                    |--->group by tb_2_.STORE_ID
                    |) tb_1_ 
                    |where tb_1_.c2 > ? 
                    |order by tb_1_.c1 asc""".trimMargin()
            )
        }
    }

    @Test
    fun testAggregateTupleAsCteBaseTable() {
        val baseTable = cteBaseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
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
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table.bookCount gt 2L)
                select(table.storeId, table.avgPrice)
            }
        ) {
            sql(
                """with tb_1_(c1, c2, c3) as (
                    |--->select 
                    |--->--->tb_2_.STORE_ID, 
                    |--->--->count(1), 
                    |--->--->avg(tb_2_.PRICE) 
                    |--->from BOOK tb_2_ 
                    |--->group by tb_2_.STORE_ID
                    |) 
                    |select tb_1_.c1, tb_1_.c3 
                    |from tb_1_ 
                    |where tb_1_.c2 > ?""".trimMargin()
            )
        }
    }

    @Test
    fun testEntityTupleAsBaseTable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.edition eq 3)
                select(
                    EntityTupleMapper
                        .book(table)
                        .authorCount(
                            subQuery(Author::class) {
                                where(table.books.id eq parentTable.id)
                                selectCount()
                            }
                        )
                )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table.authorCount gt 1L)
                select(table.book)
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 
                    |from (
                    |--->select 
                    |--->--->tb_2_.ID c1, 
                    |--->--->tb_2_.NAME c2, 
                    |--->--->tb_2_.EDITION c3, 
                    |--->--->tb_2_.PRICE c4, 
                    |--->--->tb_2_.STORE_ID c5, 
                    |--->--->(
                    |--->--->--->select count(1) 
                    |--->--->--->from AUTHOR tb_3_ 
                    |--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ 
                    |--->--->--->on tb_3_.ID = tb_4_.AUTHOR_ID 
                    |--->--->--->where tb_4_.BOOK_ID = tb_2_.ID
                    |--->--->) c6 
                    |--->from BOOK tb_2_ 
                    |--->where tb_2_.EDITION = ?
                    |) tb_1_ 
                    |where tb_1_.c6 > ?""".trimMargin()
            )
        }
    }

    @Test
    fun testWideTupleAsBaseTable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                select(
                    WideTupleMapper
                        .value1(rowCount())
                        .value2(rowCount())
                        .value3(rowCount())
                        .value4(rowCount())
                        .value5(rowCount())
                        .value6(rowCount())
                        .value7(rowCount())
                        .value8(rowCount())
                        .value9(rowCount())
                        .value10(rowCount())
                )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                select(table.value10)
            }
        ) {
            sql(
                """select tb_1_.c1 
                    |from (
                    |--->select count(1) c1 
                    |--->from BOOK tb_2_
                    |) tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testAggregateTupleWeakOuterJoin() {
        val left = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.storeId eq 1L)
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
        }
        val right = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.storeId eq 2L)
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
        }
        executeAndExpect(
            sqlClient.createQuery(left) {
                val joined = table.weakOuterJoin(right) {
                    source.storeId eq target.storeId
                }
                select(table.storeId, joined.avgPrice)
            }
        ) {
            sql(
                """select tb_1_.c1, tb_2_.c3 
                    |from (
                    |--->select tb_3_.STORE_ID c1 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.STORE_ID = ? 
                    |--->group by tb_3_.STORE_ID
                    |) tb_1_ 
                    |left join (
                    |--->select tb_4_.STORE_ID c2, avg(tb_4_.PRICE) c3 
                    |--->from BOOK tb_4_ 
                    |--->where tb_4_.STORE_ID = ? 
                    |--->group by tb_4_.STORE_ID
                    |) tb_2_ on tb_1_.c1 = tb_2_.c2""".trimMargin()
            )
        }
    }

    @Test
    fun testAggregateTupleChainedUnionAll() {
        fun query(storeId: Long) =
            sqlClient.createBaseQuery(Book::class) {
                where(table.storeId eq storeId)
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

        val union = baseTableSymbol {
            query(1L) unionAll query(2L) unionAll query(1L)
        }
        executeAndExpect(
            sqlClient.createQuery(union) {
                select(table.storeId, table.bookCount)
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c2 
                    |from (
                    |--->select tb_2_.STORE_ID c1, count(1) c2 
                    |--->from BOOK tb_2_ 
                    |--->where tb_2_.STORE_ID = ? 
                    |--->group by tb_2_.STORE_ID 
                    |--->union all 
                    |--->select tb_3_.STORE_ID c1, count(1) c2 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.STORE_ID = ? 
                    |--->group by tb_3_.STORE_ID 
                    |--->union all 
                    |--->select tb_4_.STORE_ID c1, count(1) c2 
                    |--->from BOOK tb_4_ 
                    |--->where tb_4_.STORE_ID = ? 
                    |--->group by tb_4_.STORE_ID
                    |) tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testRecursiveTupleAsCteBaseTable() {
        val baseTable = cteBaseTableSymbol {
            sqlClient.createBaseQuery(TreeNode::class) {
                where(table.parentId.isNull())
                select(
                    RecursiveTupleMapper
                        .node(table)
                        .depth(constant(1))
                )
            }.unionAllRecursively {
                sqlClient.createBaseQuery(TreeNode::class, it, {
                    source.parentId eq target.node.id
                }) {
                    select(
                        RecursiveTupleMapper
                            .node(table)
                            .depth(recursive.depth + 1)
                    )
                }
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                orderBy(table.depth, table.node.name)
                select(
                    table.node.fetchBy {
                        name()
                    },
                    table.depth
                )
            }
        ) {
            sql(
                """with recursive tb_1_(c1, c2, c3) as (
                    |--->select tb_2_.NODE_ID, tb_2_.NAME, 1 
                    |--->from TREE_NODE tb_2_ 
                    |--->where tb_2_.PARENT_ID is null 
                    |--->union all 
                    |--->select tb_3_.NODE_ID, tb_3_.NAME, tb_1_.c3 + ? 
                    |--->from TREE_NODE tb_3_ 
                    |--->inner join tb_1_ on tb_3_.PARENT_ID = tb_1_.c1
                    |) 
                    |select tb_1_.c1, tb_1_.c2, tb_1_.c3 
                    |from tb_1_ 
                    |order by tb_1_.c3 asc, tb_1_.c2 asc""".trimMargin()
            )
        }
    }

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
                orderBy(table.id)
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
                    |--->where tb_1_.EDITION = ? 
                    |--->order by tb_1_.ID asc""".trimMargin()
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
