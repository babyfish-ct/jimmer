package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.firstName
import org.babyfish.jimmer.sql.kt.model.classic.author.id
import org.babyfish.jimmer.sql.kt.model.classic.author.lastName
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test

class JoinTest : AbstractQueryTest() {

    @Test
    fun testImplicitJoinByIdView() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.storeId valueIn listOf(2L, 3L))
                select(table)
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ where tb_1_.STORE_ID in (?, ?)"
            )
        }
    }

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
                    |from BOOK tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testUnusedWeakJoin2() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                table.asTableEx().weakJoin(BookAuthorWeakJoin2::class)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_""".trimMargin()
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
                    |from BOOK tb_1_ 
                    |inner join AUTHOR tb_2_ on exists(
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
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"storeId":1
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testWeakJoin2() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.asTableEx().weakJoin(BookAuthorWeakJoin2::class).firstName eq "Alex")
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join AUTHOR tb_2_ on exists(
                    |--->select 1 from BOOK_AUTHOR_MAPPING tb_3_ 
                    |--->where tb_3_.BOOK_ID = tb_1_.ID and tb_3_.AUTHOR_ID = tb_2_.ID
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
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"storeId":1
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
                    |from BOOK tb_1_ 
                    |inner join AUTHOR tb_2_ on exists(
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
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"storeId":1
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testMergeWeakJoin2() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    table.asTableEx()
                        .weakJoin(BookAuthorWeakJoin2::class)
                        .firstName eq "Alex"
                )
                where(
                    table.asTableEx()
                        .weakJoin(BookAuthorWeakJoin2::class)
                        .lastName eq "Banks"
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join AUTHOR tb_2_ on exists(
                    |--->select 1 from BOOK_AUTHOR_MAPPING tb_3_ 
                    |--->where tb_3_.BOOK_ID = tb_1_.ID and tb_3_.AUTHOR_ID = tb_2_.ID
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
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":2,
                    |--->--->"price":55.00,
                    |--->--->"storeId":1
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":3,
                    |--->--->"price":51.00,
                    |--->--->"storeId":1
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

    private class BookAuthorWeakJoin2 : KWeakJoin<Book, Author>() {
        override fun on(
            source: KNonNullTable<Book>,
            target: KNonNullTable<Author>,
            ctx: Context<Book, Author>
        ): KNonNullExpression<Boolean> =
            exists(
                ctx.sourceWildSubQueries.forList(Book::authors) {
                    where += table.source eq parentTable
                    where += table.target eq target
                }
            )
    }

    @Test
    fun testMergeJoinsOfAnd() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    and(
                        table.store.name eq "MANNING",
                        table.`store?`.name eq "MANNING",
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_2_.NAME = ? and tb_2_.NAME = ?""".trimMargin()
            )
        }
    }

    @Test
    fun testMergeJoinsOfOr() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    or(
                        table.store.name eq "MANNING",
                        table.`store?`.name eq "MANNING",
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |left join BOOK_STORE tb_3_ on tb_1_.STORE_ID = tb_3_.ID 
                    |where tb_2_.NAME = ? or tb_3_.NAME = ?""".trimMargin()
            )
        }
    }

    @Test
    fun testSameJoinsOfOr() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    or(
                        table.`store?`.name eq "MANNING",
                        table.`store?`.name eq "MANNING",
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_2_.NAME = ? or tb_2_.NAME = ?""".trimMargin()
            )
        }
    }
}