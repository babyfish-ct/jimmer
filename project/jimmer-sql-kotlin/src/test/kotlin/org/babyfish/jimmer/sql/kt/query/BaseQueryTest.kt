package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.*
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.fetchBy
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaseQueryTest : AbstractQueryTest() {

    @Test
    fun testBaseQueryWithFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(BookStore::class) {
                selections
                    .add(table)
                    .add(
                        sql(Int::class, "dense_rank() over(order by %e desc)") {
                            expression(
                                subQuery(Book::class) {
                                    where(table.storeId eq parentTable.id)
                                    select(rowCount())
                                }
                            )
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 le 2)
                where(table._1.name like "M")
                select(table._1.fetchBy {
                    allScalarFields()
                    books {
                        allScalarFields()
                    }
                })
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 
                    |from (
                    |--->select 
                    |--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.VERSION c3, tb_2_.WEBSITE c4, 
                    |--->--->dense_rank() over(
                    |--->--->--->order by (
                    |--->--->--->--->select count(1) from BOOK tb_3_ 
                    |--->--->--->--->where tb_3_.STORE_ID = tb_2_.ID
                    |--->--->--->) desc
                    |--->--->) c5 
                    |--->from BOOK_STORE tb_2_
                    |) tb_1_ 
                    |where tb_1_.c5 <= ? and tb_1_.c2 like ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK tb_1_ where tb_1_.STORE_ID = ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":2,
                    |--->"name":"MANNING",
                    |--->"version":0,
                    |--->"website":null,
                    |--->"books":[
                    |--->--->{"id":10,"name":"GraphQL in Action","edition":1,"price":80.00},
                    |--->--->{"id":11,"name":"GraphQL in Action","edition":2,"price":81.00},
                    |--->{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}
                    |--->]
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testBaseQueryWithJoinFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                selections
                    .add(table)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 1)
                select(
                    table._1.fetchBy {
                        allScalarFields()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                    "--->tb_6_.ID, tb_6_.NAME, tb_6_.VERSION, tb_6_.WEBSITE " +
                    "--->from (" +
                    "--->--->select " +
                    "--->--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, tb_2_.STORE_ID c5, " +
                    "--->--->--->(" +
                    "--->--->--->--->select count(1) " +
                    "--->--->--->--->from AUTHOR tb_3_ " +
                    "--->--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                    "--->--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                    "--->--->--->) c6 " +
                    "--->--->from BOOK tb_2_" +
                    "--->) tb_1_ " +
                    "--->left join BOOK_STORE tb_6_ on tb_1_.c5 = tb_6_.ID " +
                    "--->where tb_1_.c6 > ?"
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":1," +
                    "--->--->\"price\":50.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null}" +
                    "--->},{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":2," +
                    "--->--->\"price\":55.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null}" +
                    "--->},{" +
                    "--->--->\"id\":3," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":3," +
                    "--->--->\"price\":51.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null" +
                    "--->--->}" +
                    "--->}" +
                    "]"
            )
        }
    }

    @Test
    fun testWeakJoinBaseTable() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(table.weakJoin(baseAuthor, BaseBookAuthorJoin::class)._1.firstName.isNotNull())
                select(
                    table._1,
                    table.weakJoin(baseAuthor, BaseBookAuthorJoin::class)._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, 
                    |--->tb_2_.c6, tb_2_.c7, tb_2_.c8, tb_2_.c9 
                    |from (
                    |--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.ID = ? 
                    |--->union all 
                    |--->select tb_4_.ID c1, tb_4_.NAME c2, tb_4_.EDITION c3, tb_4_.PRICE c4, tb_4_.STORE_ID c5 
                    |--->from BOOK tb_4_ where tb_4_.ID = ?
                    |) tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_7_ on tb_1_.c1 = tb_7_.BOOK_ID 
                    |inner join (
                    |--->select tb_5_.ID c6, tb_5_.FIRST_NAME c7, tb_5_.LAST_NAME c8, tb_5_.GENDER c9 
                    |--->from AUTHOR tb_5_ 
                    |--->where tb_5_.ID = ? 
                    |--->union all 
                    |--->select tb_6_.ID c6, tb_6_.FIRST_NAME c7, tb_6_.LAST_NAME c8, tb_6_.GENDER c9 
                    |--->from AUTHOR tb_6_ 
                    |--->where tb_6_.ID = ?
                    |) tb_2_ on tb_7_.AUTHOR_ID = tb_2_.c6 
                    |where tb_2_.c7 is not null""".trimMargin()
            )
        }
    }

    @Test
    fun testLambdaWeakJoinBaseTable() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(
                    table.weakJoin(baseAuthor) {
                        source._1.asTableEx().authors eq target._1
                    }._1.firstName.isNotNull()
                )
                select(
                    table._1,
                    table.weakJoin(baseAuthor) {
                        source._1.asTableEx().authors eq target._1
                    }._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, 
                    |--->tb_2_.c6, tb_2_.c7, tb_2_.c8, tb_2_.c9 
                    |from (
                    |--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.ID = ? 
                    |--->union all 
                    |--->select tb_4_.ID c1, tb_4_.NAME c2, tb_4_.EDITION c3, tb_4_.PRICE c4, tb_4_.STORE_ID c5 
                    |--->from BOOK tb_4_ where tb_4_.ID = ?
                    |) tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_7_ on tb_1_.c1 = tb_7_.BOOK_ID 
                    |inner join (
                    |--->select tb_5_.ID c6, tb_5_.FIRST_NAME c7, tb_5_.LAST_NAME c8, tb_5_.GENDER c9 
                    |--->from AUTHOR tb_5_ 
                    |--->where tb_5_.ID = ? 
                    |--->union all 
                    |--->select tb_6_.ID c6, tb_6_.FIRST_NAME c7, tb_6_.LAST_NAME c8, tb_6_.GENDER c9 
                    |--->from AUTHOR tb_6_ 
                    |--->where tb_6_.ID = ?
                    |) tb_2_ on tb_7_.AUTHOR_ID = tb_2_.c6 
                    |where tb_2_.c7 is not null""".trimMargin()
            )
        }
    }

    private class BaseBookAuthorJoin : KPropsWeakJoin<
        KNonNullBaseTable1<KNonNullTable<Book>, KNullableTable<Book>>,
        KNonNullBaseTable1<KNonNullTable<Author>, KNullableTable<Author>>
    >() {
        override fun on(
            source: KNonNullBaseTable1<KNonNullTable<Book>, KNullableTable<Book>>,
            target: KNonNullBaseTable1<KNonNullTable<Author>, KNullableTable<Author>>
        ): KNonNullExpression<Boolean> =
            source._1.asTableEx().authors.eq(target._1)
    }

    @Test
    fun testTableWeakJoinBaseTable() {
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.gender eq Gender.MALE)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                select(
                    table,
                    table.asTableEx().weakJoin(baseAuthor) {
                        source.id eq target._1.asTableEx().books.id
                    }._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, 
                    |--->tb_2_.c1, tb_2_.c2, tb_2_.c3, tb_2_.c4 
                    |from BOOK tb_1_ 
                    |inner join (
                    |--->(
                    |--->--->select tb_3_.ID c1, tb_3_.FIRST_NAME c2, tb_3_.LAST_NAME c3, tb_3_.GENDER c4 
                    |--->--->from AUTHOR tb_3_ 
                    |--->--->where tb_3_.GENDER = ?
                    |--->) tb_2_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_4_ 
                    |--->--->on tb_2_.c1 = tb_4_.AUTHOR_ID
                    |) on tb_1_.ID = tb_4_.BOOK_ID""".trimMargin()
            )
        }
    }

    @Test
    fun testIllegalTableWeakJoinBaseTable() {
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.gender eq Gender.MALE)
                selections.add(table)
            }
        }
        assertFailsWith(
            IllegalStateException::class,
            "Table join is disabled. " +
                "For the weak join operation from a regular table to a base table, " +
                "the strong join is not allowed on the regular table side (source side)"
        ) {
            executeAndExpect(
                sqlClient.createQuery(Book::class) {
                    select(
                        table,
                        table.asTableEx().weakJoin(baseAuthor) {
                            source.asTableEx().authors.id eq target._1.id
                        }._1
                    )
                }
            ) {}
        }
    }
}