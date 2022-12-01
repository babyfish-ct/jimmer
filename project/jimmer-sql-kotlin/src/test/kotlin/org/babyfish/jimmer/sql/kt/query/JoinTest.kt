package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KWeakJoin
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import kotlin.test.Test

class JoinTest : AbstractQueryTest() {

    @Test
    fun testUnusedWeakJoin() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                table.asTableEx().weakJoin(BookAuthorWeakJoin::class)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testWeakJoin() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.asTableEx().weakJoin(BookAuthorWeakJoin::class).firstName eq "Alex")
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |inner join AUTHOR as tb_2_ on exists(
                    |--->select * from book_author_mapping 
                    |--->where book_id = tb_1_.ID and author_id = tb_2_.ID
                    |) 
                    |where tb_2_.FIRST_NAME = ?""".trimMargin()
            ).variables("Alex")
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
    fun testMergeWeakJoin() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    table.asTableEx()
                        .weakJoin(BookAuthorWeakJoin::class)
                        .firstName eq "Alex"
                )
                where(
                    table.asTableEx()
                        .weakJoin(BookAuthorWeakJoin::class)
                        .lastName eq "Banks"
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |inner join AUTHOR as tb_2_ on exists(
                    |--->select * from book_author_mapping 
                    |--->where book_id = tb_1_.ID and author_id = tb_2_.ID
                    |) 
                    |where tb_2_.FIRST_NAME = ? and tb_2_.LAST_NAME = ?""".trimMargin()
            ).variables("Alex", "Banks")
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

    private class BookAuthorWeakJoin : KWeakJoin<Book, Author>() {

        override fun on(
            source: KNonNullTable<Book>,
            target: KNonNullTable<Author>
        ): KNonNullExpression<Boolean> =
            sql(
                Boolean::class,
                "exists(select * from book_author_mapping " +
                    "where book_id = %e and author_id = %e)"
            ) {
                expression(source.id)
                expression(target.id)
            }
    }
}