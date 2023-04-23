package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.exists
import org.babyfish.jimmer.sql.kt.ast.table.source
import org.babyfish.jimmer.sql.kt.ast.table.target
import org.babyfish.jimmer.sql.kt.model.classic.author.firstName
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.junit.Test

class AssociationQueryTest : AbstractQueryTest() {

    @Test
    fun testMiddleTableOnly() {
        executeAndExpect(
            sqlClient.queries.forList(Book::authors) {
                where(table.target.firstName eq "Alex")
                select(table)
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.BOOK_ID, tb_1_.AUTHOR_ID 
                    |from BOOK_AUTHOR_MAPPING tb_1_ 
                    |inner join AUTHOR tb_2_ 
                    |--->on tb_1_.AUTHOR_ID = tb_2_.ID 
                    |where tb_2_.FIRST_NAME = ?""".trimMargin()
            )
            variables("Alex")
            rows {
                contentEquals(
                    """[
                        |--->Association{source={"id":1}, target={"id":2}}, 
                        |--->Association{source={"id":2}, target={"id":2}}, 
                        |--->Association{source={"id":3}, target={"id":2}}
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testMiddleTableTargets() {
        executeAndExpect(
            sqlClient.queries.forList(Book::authors) {
                where(table.target.firstName eq "Alex")
                select(table.source, table.target)
            }
        ) {
            sql(
                """select 
                        |--->tb_1_.BOOK_ID, tb_3_.NAME, tb_3_.EDITION, tb_3_.PRICE, tb_3_.STORE_ID, 
                        |--->tb_1_.AUTHOR_ID, tb_2_.FIRST_NAME, tb_2_.LAST_NAME, tb_2_.GENDER 
                        |from BOOK_AUTHOR_MAPPING tb_1_ 
                        |inner join AUTHOR tb_2_ on tb_1_.AUTHOR_ID = tb_2_.ID 
                        |inner join BOOK tb_3_ on tb_1_.BOOK_ID = tb_3_.ID 
                        |where tb_2_.FIRST_NAME = ?""".trimMargin()
            )
            variables("Alex")
            rows {
                contentEquals(
                    """[
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2={
                        |--->--->--->"id":2,
                        |--->--->--->"firstName":"Alex",
                        |--->--->--->"lastName":"Banks",
                        |--->--->--->"gender":"MALE"
                        |--->--->}
                        |--->), 
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":2,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":55.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2={
                        |--->--->--->"id":2,
                        |--->--->--->"firstName":"Alex",
                        |--->--->--->"lastName":"Banks",
                        |--->--->--->"gender":"MALE"
                        |--->--->}
                        |--->), 
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":3,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":51.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, 
                        |--->--->_2={
                        |--->--->--->"id":2,
                        |--->--->--->"firstName":"Alex",
                        |--->--->--->"lastName":"Banks",
                        |--->--->--->"gender":"MALE"
                        |--->--->}
                        |--->)
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testBySubQuery() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    exists(
                        subQueries.forList(Book::authors) {
                            where(
                                table.source eq parentTable,
                                table.target.firstName eq "Alex"
                            )
                            select(constant(1))
                        }
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where exists(
                    |--->select 1 
                    |--->from BOOK_AUTHOR_MAPPING tb_2_ 
                    |--->inner join AUTHOR tb_4_ on tb_2_.AUTHOR_ID = tb_4_.ID 
                    |--->where 
                    |--->--->tb_2_.BOOK_ID = tb_1_.ID 
                    |--->and 
                    |--->--->tb_4_.FIRST_NAME = ?
                    |)""".trimMargin()
            )
            variables("Alex")
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":1,
                    |--->--->"price":50.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"store":{"id":1}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testByWildSubQuery() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    exists(
                        wildSubQueries.forList(Book::authors) {
                            where(
                                table.source eq parentTable,
                                table.target.firstName eq "Alex"
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
                    |where exists(
                    |--->select 1 
                    |--->from BOOK_AUTHOR_MAPPING tb_2_ 
                    |--->inner join AUTHOR tb_4_ on tb_2_.AUTHOR_ID = tb_4_.ID 
                    |--->where 
                    |--->--->tb_2_.BOOK_ID = tb_1_.ID 
                    |--->and 
                    |--->--->tb_4_.FIRST_NAME = ?
                    |)""".trimMargin()
            )
            variables("Alex")
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":1,
                    |--->--->"price":50.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"store":{"id":1}
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"store":{"id":1}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }
}